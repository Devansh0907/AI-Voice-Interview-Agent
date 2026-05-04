import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { HiOutlineMicrophone, HiOutlineLightningBolt, HiOutlineChartBar, HiOutlineChat } from 'react-icons/hi';
import { useAuth } from '../context/AuthContext';
import './Landing.css';

export default function Landing() {
  const { isAuthenticated } = useAuth();

  const features = [
    { icon: <HiOutlineMicrophone />, title: 'Voice Interviews', desc: 'Speak naturally — Whisper AI transcribes your answers in real-time' },
    { icon: <HiOutlineLightningBolt />, title: 'AI-Powered Questions', desc: 'Gemini generates tailored questions based on your resume & experience' },
    { icon: <HiOutlineChat />, title: 'RAG Context', desc: 'Questions adapt to YOUR profile using retrieval-augmented generation' },
    { icon: <HiOutlineChartBar />, title: 'Detailed Feedback', desc: 'Get scored across technical, communication, and problem-solving skills' },
  ];

  return (
    <div className="landing">
      <div className="bg-orb bg-orb-1" />
      <div className="bg-orb bg-orb-2" />

      {/* Hero */}
      <section className="hero">
        <motion.div className="hero-content"
          initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
          <div className="hero-badge">
            <span className="badge badge-purple">✨ AI-Powered Interview Platform</span>
          </div>
          <h1 className="hero-title">
            Ace Your Next<br />
            <span className="gradient-text">Technical Interview</span>
          </h1>
          <p className="hero-subtitle">
            Practice with an AI interviewer that adapts to your resume. Get real-time
            voice-based interviews powered by Whisper & Gemini AI with detailed feedback.
          </p>
          <div className="hero-actions">
            <Link to={isAuthenticated ? '/dashboard' : '/register'} className="btn btn-primary btn-lg">
              {isAuthenticated ? 'Go to Dashboard' : 'Start Practicing Free'} →
            </Link>
            <Link to={isAuthenticated ? '/interview-setup' : '/login'} className="btn btn-secondary btn-lg">
              {isAuthenticated ? 'New Interview' : 'Login'}
            </Link>
          </div>
        </motion.div>

        {/* Animated Waveform */}
        <motion.div className="hero-visual"
          initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8, delay: 0.3 }}>
          <div className="waveform-container">
            {Array.from({ length: 30 }).map((_, i) => (
              <div key={i} className="waveform-bar"
                style={{ animationDelay: `${i * 0.05}s`, height: `${Math.random() * 60 + 10}%` }} />
            ))}
          </div>
          <div className="visual-label">AI Interview in Progress...</div>
        </motion.div>
      </section>

      {/* Features */}
      <section className="features-section">
        <div className="page-container">
          <h2 className="section-title">How It Works</h2>
          <div className="grid-4 features-grid">
            {features.map((f, i) => (
              <motion.div key={i} className="feature-card glass-card"
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 * i + 0.5 }}>
                <div className="feature-icon">{f.icon}</div>
                <h3>{f.title}</h3>
                <p>{f.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
