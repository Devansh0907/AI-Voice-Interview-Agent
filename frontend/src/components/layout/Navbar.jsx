import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { HiOutlineMicrophone, HiOutlineUser, HiOutlineLogout, HiOutlineViewGrid } from 'react-icons/hi';
import { motion } from 'framer-motion';
import './Navbar.css';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <motion.nav className="navbar" initial={{ y: -60 }} animate={{ y: 0 }} transition={{ duration: 0.4 }}>
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <div className="brand-icon">
            <HiOutlineMicrophone />
          </div>
          <span className="brand-text">InterviewAI</span>
        </Link>

        <div className="navbar-links">
          {isAuthenticated ? (
            <>
              <Link to="/dashboard" className={`nav-link ${isActive('/dashboard') ? 'active' : ''}`}>
                <HiOutlineViewGrid /> Dashboard
              </Link>
              <Link to="/profile" className={`nav-link ${isActive('/profile') ? 'active' : ''}`}>
                <HiOutlineUser /> Profile
              </Link>
              <div className="nav-user">
                <span className="nav-user-name">{user?.fullName}</span>
                <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
                  <HiOutlineLogout /> Logout
                </button>
              </div>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-secondary btn-sm">Login</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Get Started</Link>
            </>
          )}
        </div>
      </div>
    </motion.nav>
  );
}
