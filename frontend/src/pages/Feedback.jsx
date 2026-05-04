import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Radar } from 'react-chartjs-2';
import { Chart as ChartJS, RadialLinearScale, PointElement, LineElement, Filler, Tooltip } from 'chart.js';
import { HiOutlineArrowLeft, HiOutlineStar, HiOutlineTrendingUp, HiOutlineExclamation } from 'react-icons/hi';
import { feedbackService, interviewService } from '../services/services';
import toast from 'react-hot-toast';
import './Feedback.css';

ChartJS.register(RadialLinearScale, PointElement, LineElement, Filler, Tooltip);

export default function Feedback() {
  const { interviewId } = useParams();
  const [feedback, setFeedback] = useState(null);
  const [interview, setInterview] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const [fbRes, intRes] = await Promise.all([
          feedbackService.generateFeedback(interviewId),
          interviewService.getInterview(interviewId),
        ]);
        setFeedback(fbRes.data);
        setInterview(intRes.data);
      } catch (err) {
        toast.error('Failed to load feedback');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [interviewId]);

  if (loading) return <div className="loading-container"><div className="spinner" /></div>;
  if (!feedback) return <div className="loading-container"><p>No feedback available</p></div>;

  const radarData = {
    labels: ['Technical', 'Communication', 'Problem Solving'],
    datasets: [{
      label: 'Score',
      data: [feedback.technicalScore, feedback.communicationScore, feedback.problemSolvingScore],
      backgroundColor: 'rgba(124, 58, 237, 0.2)',
      borderColor: 'rgba(124, 58, 237, 0.8)',
      pointBackgroundColor: '#7c3aed',
      pointBorderColor: '#fff',
      pointHoverBackgroundColor: '#fff',
      pointHoverBorderColor: '#7c3aed',
      borderWidth: 2,
    }],
  };

  const radarOptions = {
    responsive: true,
    scales: {
      r: {
        beginAtZero: true,
        max: 100,
        ticks: { color: '#64748b', backdropColor: 'transparent', stepSize: 20 },
        grid: { color: 'rgba(255,255,255,0.05)' },
        angleLines: { color: 'rgba(255,255,255,0.08)' },
        pointLabels: { color: '#94a3b8', font: { size: 13, weight: '600' } },
      },
    },
    plugins: { tooltip: { enabled: true } },
  };

  const scoreColor = (score) => score >= 70 ? 'var(--accent-emerald)' : score >= 40 ? 'var(--accent-amber)' : 'var(--accent-rose)';

  return (
    <div className="page-container feedback-page">
      <Link to="/dashboard" className="back-link"><HiOutlineArrowLeft /> Back to Dashboard</Link>

      <div className="page-header">
        <h1>Interview Feedback</h1>
        <p>{interview?.topic} • {interview?.difficulty} • {interview?.totalQuestions} questions</p>
      </div>

      {/* Overall Score */}
      <motion.div className="score-hero glass-card"
        initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}>
        <div className="score-circle" style={{ '--score-color': scoreColor(feedback.overallScore) }}>
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="54" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="8" />
            <circle cx="60" cy="60" r="54" fill="none" stroke="url(#scoreGrad)" strokeWidth="8"
              strokeDasharray={`${(feedback.overallScore / 100) * 339} 339`}
              strokeLinecap="round" transform="rotate(-90 60 60)" />
            <defs><linearGradient id="scoreGrad" x1="0%" y1="0%" x2="100%">
              <stop offset="0%" stopColor="#7c3aed" /><stop offset="100%" stopColor="#06b6d4" />
            </linearGradient></defs>
          </svg>
          <div className="score-text">
            <div className="score-number">{feedback.overallScore}</div>
            <div className="score-label">Overall</div>
          </div>
        </div>
        <div className="score-breakdown">
          {[
            { label: 'Technical', score: feedback.technicalScore },
            { label: 'Communication', score: feedback.communicationScore },
            { label: 'Problem Solving', score: feedback.problemSolvingScore },
          ].map((item, i) => (
            <div key={i} className="breakdown-item">
              <div className="breakdown-header">
                <span>{item.label}</span>
                <span style={{ color: scoreColor(item.score) }}>{item.score}%</span>
              </div>
              <div className="breakdown-bar">
                <div className="breakdown-fill" style={{ width: `${item.score}%` }} />
              </div>
            </div>
          ))}
        </div>
      </motion.div>

      <div className="feedback-grid">
        {/* Radar Chart */}
        <motion.div className="glass-card chart-card"
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}>
          <h3>Performance Radar</h3>
          <div className="chart-container">
            <Radar data={radarData} options={radarOptions} />
          </div>
        </motion.div>

        {/* Strengths & Weaknesses */}
        <div className="sw-cards">
          <motion.div className="glass-card sw-card"
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }}>
            <h3><HiOutlineStar className="icon-emerald" /> Strengths</h3>
            <div className="pill-list">
              {feedback.strengths?.split(',').map((s, i) => (
                <span key={i} className="badge badge-emerald">{s.trim()}</span>
              ))}
            </div>
          </motion.div>

          <motion.div className="glass-card sw-card"
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.35 }}>
            <h3><HiOutlineExclamation className="icon-rose" /> Areas to Improve</h3>
            <div className="pill-list">
              {feedback.weaknesses?.split(',').map((w, i) => (
                <span key={i} className="badge badge-rose">{w.trim()}</span>
              ))}
            </div>
          </motion.div>

          <motion.div className="glass-card sw-card"
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }}>
            <h3><HiOutlineTrendingUp className="icon-teal" /> Improvement Areas</h3>
            <div className="pill-list">
              {feedback.improvementAreas?.split(',').map((a, i) => (
                <span key={i} className="badge badge-teal">{a.trim()}</span>
              ))}
            </div>
          </motion.div>
        </div>
      </div>

      {/* Overall Feedback */}
      <motion.div className="glass-card overall-feedback"
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.45 }}>
        <h3>Overall Feedback</h3>
        <p>{feedback.overallFeedback}</p>
      </motion.div>

      {/* Per-question Review */}
      {interview?.questions && (
        <div className="questions-review">
          <h3>Question-by-Question Review</h3>
          {interview.questions.filter(q => q.answerText).map((q, i) => (
            <motion.div key={q.id} className="glass-card qa-card"
              initial={{ opacity: 0, y: 15 }} animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.05 * i + 0.5 }}>
              <div className="qa-header">
                <span className="qa-num">Q{q.questionOrder}</span>
                {q.score && <span className={`score-badge ${q.score >= 7 ? 'good' : q.score >= 4 ? 'mid' : 'low'}`}>
                  {q.score}/10
                </span>}
              </div>
              <p className="qa-question">{q.questionText}</p>
              <p className="qa-answer"><strong>Your Answer:</strong> {q.answerText}</p>
              {q.feedback && <p className="qa-feedback">{q.feedback}</p>}
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
