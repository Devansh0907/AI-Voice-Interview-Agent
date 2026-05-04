import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { HiOutlineLightningBolt, HiOutlineAcademicCap, HiOutlineFire } from 'react-icons/hi';
import { interviewService } from '../services/services';
import toast from 'react-hot-toast';
import './InterviewSetup.css';

const TOPICS = [
  'Java & Spring Boot', 'React & Frontend', 'System Design', 'Data Structures & Algorithms',
  'Python & Django', 'Cloud & DevOps', 'Databases & SQL', 'General Software Engineering',
];

const DIFFICULTIES = [
  { value: 'EASY', label: 'Easy', icon: <HiOutlineAcademicCap />, desc: 'Foundational concepts' },
  { value: 'MEDIUM', label: 'Medium', icon: <HiOutlineLightningBolt />, desc: 'Standard interview level' },
  { value: 'HARD', label: 'Hard', icon: <HiOutlineFire />, desc: 'Senior/Lead level' },
];

export default function InterviewSetup() {
  const [topic, setTopic] = useState('General Software Engineering');
  const [difficulty, setDifficulty] = useState('MEDIUM');
  const [maxQuestions, setMaxQuestions] = useState(10);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleStart = async () => {
    setLoading(true);
    try {
      const { data } = await interviewService.startInterview({ topic, difficulty, maxQuestions });
      toast.success('Interview started! 🎤');
      navigate(`/interview/${data.id}`);
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to start interview');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Interview Setup</h1>
        <p>Configure your interview session before we begin</p>
      </div>

      <div className="setup-grid">
        {/* Topic */}
        <motion.div className="setup-card glass-card"
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <h3>Select Topic</h3>
          <div className="topic-grid">
            {TOPICS.map((t) => (
              <button key={t} className={`topic-chip ${topic === t ? 'active' : ''}`}
                onClick={() => setTopic(t)}>{t}</button>
            ))}
          </div>
        </motion.div>

        {/* Difficulty */}
        <motion.div className="setup-card glass-card"
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          <h3>Difficulty Level</h3>
          <div className="difficulty-options">
            {DIFFICULTIES.map((d) => (
              <button key={d.value} className={`difficulty-card ${difficulty === d.value ? 'active' : ''}`}
                onClick={() => setDifficulty(d.value)}>
                <div className="diff-icon">{d.icon}</div>
                <div className="diff-label">{d.label}</div>
                <div className="diff-desc">{d.desc}</div>
              </button>
            ))}
          </div>
        </motion.div>

        {/* Questions */}
        <motion.div className="setup-card glass-card"
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}>
          <h3>Number of Questions</h3>
          <div className="question-slider">
            <input type="range" min="3" max="15" value={maxQuestions}
              onChange={(e) => setMaxQuestions(parseInt(e.target.value))}
              className="slider" />
            <div className="slider-value">{maxQuestions} questions</div>
          </div>
        </motion.div>
      </div>

      <motion.div className="setup-actions"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.3 }}>
        <button className="btn btn-primary btn-lg" onClick={handleStart} disabled={loading}>
          {loading ? 'Starting Interview...' : '🎤 Start Interview'}
        </button>
      </motion.div>
    </div>
  );
}
