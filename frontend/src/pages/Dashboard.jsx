import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { HiOutlinePlus, HiOutlineClock, HiOutlineStar, HiOutlineChartBar, HiOutlineArrowRight } from 'react-icons/hi';
import { interviewService } from '../services/services';
import './Dashboard.css';

export default function Dashboard() {
  const [interviews, setInterviews] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    interviewService.getInterviews()
      .then(({ data }) => setInterviews(data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const completed = interviews.filter(i => i.status === 'COMPLETED');
  const avgScore = completed.length > 0
    ? Math.round(completed.reduce((sum, i) => sum + (i.feedback?.overallScore || 0), 0) / completed.length)
    : 0;

  const stats = [
    { label: 'Total Interviews', value: interviews.length, icon: <HiOutlineClock /> },
    { label: 'Completed', value: completed.length, icon: <HiOutlineStar /> },
    { label: 'Avg Score', value: avgScore + '%', icon: <HiOutlineChartBar /> },
  ];

  if (loading) return <div className="loading-container"><div className="spinner" /></div>;

  return (
    <div className="page-container">
      <div className="bg-orb bg-orb-1" />
      <div className="dashboard-header">
        <div className="page-header">
          <h1>Dashboard</h1>
          <p>Track your interview practice progress</p>
        </div>
        <Link to="/interview-setup" className="btn btn-primary">
          <HiOutlinePlus /> New Interview
        </Link>
      </div>

      {/* Stats */}
      <div className="grid-3 stats-row">
        {stats.map((s, i) => (
          <motion.div key={i} className="stat-card glass-card"
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}>
            <div className="stat-icon">{s.icon}</div>
            <div className="stat-value">{s.value}</div>
            <div className="stat-label">{s.label}</div>
          </motion.div>
        ))}
      </div>

      {/* Interview History */}
      <div className="history-section">
        <h2>Interview History</h2>
        {interviews.length === 0 ? (
          <div className="empty-state glass-card">
            <p>No interviews yet. Start your first AI-powered interview!</p>
            <Link to="/interview-setup" className="btn btn-primary"><HiOutlinePlus /> Start Interview</Link>
          </div>
        ) : (
          <div className="interview-list">
            {interviews.map((interview, i) => (
              <motion.div key={interview.id} className="interview-item glass-card"
                initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}>
                <div className="interview-info">
                  <h3>{interview.topic}</h3>
                  <div className="interview-meta">
                    <span className={`badge badge-${interview.status === 'COMPLETED' ? 'emerald' : 'amber'}`}>
                      {interview.status}
                    </span>
                    <span className="badge badge-purple">{interview.difficulty}</span>
                    <span className="meta-text">{interview.totalQuestions} questions</span>
                    <span className="meta-text">{new Date(interview.startedAt).toLocaleDateString()}</span>
                  </div>
                </div>
                <Link to={interview.status === 'COMPLETED' ? `/feedback/${interview.id}` : `/interview/${interview.id}`}
                  className="btn btn-secondary btn-sm">
                  {interview.status === 'COMPLETED' ? 'View Feedback' : 'Continue'} <HiOutlineArrowRight />
                </Link>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
