import { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import { useDropzone } from 'react-dropzone';
import { HiOutlineUpload, HiOutlineDocument, HiOutlineCheck } from 'react-icons/hi';
import { profileService } from '../services/services';
import toast from 'react-hot-toast';
import './Profile.css';

export default function Profile() {
  const [profile, setProfile] = useState({ title: '', experienceYears: '', skills: '', education: '' });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    profileService.getProfile()
      .then(({ data }) => setProfile(data))
      .catch(() => toast.error('Failed to load profile'))
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const { data } = await profileService.updateProfile(profile);
      setProfile(data);
      toast.success('Profile updated! RAG context refreshed ✨');
    } catch (err) {
      toast.error('Failed to save profile');
    } finally {
      setSaving(false);
    }
  };

  const onDrop = useCallback(async (acceptedFiles) => {
    if (acceptedFiles.length === 0) return;
    setUploading(true);
    try {
      const { data } = await profileService.uploadResume(acceptedFiles[0]);
      setProfile(data);
      toast.success('Resume uploaded & parsed! 📄');
    } catch (err) {
      toast.error('Upload failed');
    } finally {
      setUploading(false);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/pdf': ['.pdf'], 'text/plain': ['.txt'] },
    maxFiles: 1,
    maxSize: 10 * 1024 * 1024,
  });

  if (loading) return <div className="loading-container"><div className="spinner" /></div>;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Your Profile</h1>
        <p>Update your profile so the AI can tailor interview questions to your experience</p>
      </div>

      <div className="profile-grid">
        {/* Form */}
        <motion.form onSubmit={handleSave} className="profile-form glass-card"
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <div className="input-group">
            <label>Job Title / Role</label>
            <input type="text" className="input-field" placeholder="Senior Software Engineer"
              value={profile.title || ''} onChange={(e) => setProfile({ ...profile, title: e.target.value })} />
          </div>
          <div className="input-group">
            <label>Years of Experience</label>
            <input type="number" className="input-field" placeholder="5"
              value={profile.experienceYears || ''} onChange={(e) => setProfile({ ...profile, experienceYears: parseInt(e.target.value) || '' })} />
          </div>
          <div className="input-group">
            <label>Skills (comma-separated)</label>
            <textarea className="input-field" placeholder="Java, Spring Boot, React, AWS, System Design..."
              value={profile.skills || ''} onChange={(e) => setProfile({ ...profile, skills: e.target.value })} />
          </div>
          <div className="input-group">
            <label>Education</label>
            <textarea className="input-field" placeholder="B.Tech Computer Science, XYZ University"
              value={profile.education || ''} onChange={(e) => setProfile({ ...profile, education: e.target.value })} />
          </div>
          <button type="submit" className="btn btn-primary btn-full" disabled={saving}>
            {saving ? 'Saving...' : 'Save Profile'}
          </button>
        </motion.form>

        {/* Resume Upload */}
        <motion.div className="resume-section"
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }}>
          <div {...getRootProps()} className={`dropzone glass-card ${isDragActive ? 'drag-active' : ''}`}>
            <input {...getInputProps()} />
            <div className="dropzone-icon">
              {uploading ? <div className="spinner" /> : profile.hasResume ? <HiOutlineCheck /> : <HiOutlineUpload />}
            </div>
            <h3>{uploading ? 'Uploading...' : profile.hasResume ? 'Resume Uploaded ✓' : 'Upload Resume'}</h3>
            <p>{isDragActive ? 'Drop it here!' : 'Drag & drop a PDF or TXT file, or click to browse'}</p>
          </div>

          {profile.resumeText && (
            <div className="resume-preview glass-card">
              <h3><HiOutlineDocument /> Resume Preview</h3>
              <pre className="resume-text">{profile.resumeText.substring(0, 1000)}{profile.resumeText.length > 1000 ? '...' : ''}</pre>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}
