import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlineMicrophone, HiOutlineStop, HiOutlinePaperAirplane, HiOutlineX } from 'react-icons/hi';
import { interviewService } from '../services/services';
import { useAudioRecorder } from '../hooks/useAudioRecorder';
import toast from 'react-hot-toast';
import './Interview.css';

export default function Interview() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [interview, setInterview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [textAnswer, setTextAnswer] = useState('');
  const [mode, setMode] = useState('voice'); // voice or text
  const { isRecording, audioBlob, duration, startRecording, stopRecording, resetRecording } = useAudioRecorder();
  const chatEndRef = useRef(null);

  useEffect(() => {
    interviewService.getInterview(id)
      .then(({ data }) => setInterview(data))
      .catch(() => toast.error('Failed to load interview'))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [interview?.questions]);

  const getCurrentQuestion = () => {
    if (!interview?.questions) return null;
    return interview.questions.find(q => !q.answerText);
  };

  const handleSubmitAudio = async () => {
    if (!audioBlob) return;
    setSubmitting(true);
    try {
      const { data } = await interviewService.submitAudio(id, audioBlob);
      setInterview(data);
      resetRecording();
      toast.success('Answer submitted!');
    } catch (err) {
      toast.error('Failed to submit answer');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitText = async () => {
    if (!textAnswer.trim()) return;
    setSubmitting(true);
    try {
      const { data } = await interviewService.submitText(id, textAnswer);
      setInterview(data);
      setTextAnswer('');
      toast.success('Answer submitted!');
    } catch (err) {
      toast.error('Failed to submit answer');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEndInterview = async () => {
    try {
      await interviewService.endInterview(id);
      toast.success('Interview ended!');
      navigate(`/feedback/${id}`);
    } catch (err) {
      toast.error('Failed to end interview');
    }
  };

  const formatDuration = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  if (loading) return <div className="loading-container"><div className="spinner" /></div>;
  if (!interview) return <div className="loading-container"><p>Interview not found</p></div>;

  const currentQ = getCurrentQuestion();
  const answeredCount = interview.questions.filter(q => q.answerText).length;
  const progress = (answeredCount / interview.maxQuestions) * 100;

  return (
    <div className="interview-page">
      {/* Header */}
      <div className="interview-topbar">
        <div className="topbar-info">
          <h2>{interview.topic}</h2>
          <div className="topbar-meta">
            <span className="badge badge-purple">{interview.difficulty}</span>
            <span className="meta-text">Question {answeredCount + 1} / {interview.maxQuestions}</span>
          </div>
        </div>
        <div className="topbar-progress">
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
        </div>
        <button className="btn btn-danger btn-sm" onClick={handleEndInterview}>
          <HiOutlineX /> End Interview
        </button>
      </div>

      {/* Chat Area */}
      <div className="chat-area">
        <AnimatePresence>
          {interview.questions.map((q, i) => (
            <div key={q.id}>
              {/* AI Question */}
              <motion.div className="chat-msg ai-msg"
                initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.1 }}>
                <div className="msg-avatar ai-avatar">AI</div>
                <div className="msg-bubble ai-bubble">
                  <div className="msg-label">Question {q.questionOrder}</div>
                  <p>{q.questionText}</p>
                </div>
              </motion.div>

              {/* User Answer */}
              {q.answerText && (
                <motion.div className="chat-msg user-msg"
                  initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }}>
                  <div className="msg-bubble user-bubble">
                    <p>{q.answerText}</p>
                    {q.score && (
                      <div className="answer-score">
                        <span className={`score-badge ${q.score >= 7 ? 'good' : q.score >= 4 ? 'mid' : 'low'}`}>
                          Score: {q.score}/10
                        </span>
                      </div>
                    )}
                  </div>
                  <div className="msg-avatar user-avatar">You</div>
                </motion.div>
              )}
            </div>
          ))}
        </AnimatePresence>

        {!currentQ && answeredCount > 0 && (
          <motion.div className="chat-msg ai-msg"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
            <div className="msg-avatar ai-avatar">AI</div>
            <div className="msg-bubble ai-bubble">
              <p>Great job! You've completed all questions. Click "End Interview" to get your detailed feedback report.</p>
            </div>
          </motion.div>
        )}

        <div ref={chatEndRef} />
      </div>

      {/* Input Area */}
      {currentQ && (
        <div className="input-area glass-card">
          <div className="mode-toggle">
            <button className={`mode-btn ${mode === 'voice' ? 'active' : ''}`}
              onClick={() => setMode('voice')}>🎤 Voice</button>
            <button className={`mode-btn ${mode === 'text' ? 'active' : ''}`}
              onClick={() => setMode('text')}>⌨️ Text</button>
          </div>

          {mode === 'voice' ? (
            <div className="voice-input">
              {!isRecording && !audioBlob && (
                <button className="record-btn" onClick={startRecording} disabled={submitting}>
                  <HiOutlineMicrophone /> Tap to Record
                </button>
              )}
              {isRecording && (
                <div className="recording-active">
                  <div className="recording-indicator">
                    <div className="rec-dot" />
                    <span>Recording... {formatDuration(duration)}</span>
                  </div>
                  <div className="rec-waveform">
                    {Array.from({ length: 20 }).map((_, i) => (
                      <div key={i} className="rec-bar" style={{ animationDelay: `${i * 0.05}s` }} />
                    ))}
                  </div>
                  <button className="btn btn-danger" onClick={stopRecording}>
                    <HiOutlineStop /> Stop
                  </button>
                </div>
              )}
              {audioBlob && !isRecording && (
                <div className="audio-ready">
                  <span>✓ Audio recorded ({formatDuration(duration)})</span>
                  <div className="audio-actions">
                    <button className="btn btn-secondary" onClick={resetRecording}>Re-record</button>
                    <button className="btn btn-primary" onClick={handleSubmitAudio} disabled={submitting}>
                      {submitting ? 'Transcribing...' : 'Submit Answer'} <HiOutlinePaperAirplane />
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="text-input">
              <textarea className="input-field" rows={3} placeholder="Type your answer here..."
                value={textAnswer} onChange={(e) => setTextAnswer(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter' && e.ctrlKey) handleSubmitText(); }} />
              <button className="btn btn-primary" onClick={handleSubmitText}
                disabled={submitting || !textAnswer.trim()}>
                {submitting ? 'Submitting...' : 'Submit'} <HiOutlinePaperAirplane />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
