import api from './api';

export const authService = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
};

export const profileService = {
  getProfile: () => api.get('/profile'),
  updateProfile: (data) => api.put('/profile', data),
  uploadResume: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/profile/resume', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export const interviewService = {
  startInterview: (data) => api.post('/interviews/start', data),
  submitAudio: (interviewId, audioBlob) => {
    const formData = new FormData();
    formData.append('audio', audioBlob, 'recording.webm');
    return api.post(`/interviews/${interviewId}/answer`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  submitText: (interviewId, answer) =>
    api.post(`/interviews/${interviewId}/answer-text`, { answer }),
  endInterview: (interviewId) => api.post(`/interviews/${interviewId}/end`),
  getInterviews: () => api.get('/interviews'),
  getInterview: (id) => api.get(`/interviews/${id}`),
};

export const feedbackService = {
  generateFeedback: (interviewId) => api.post(`/feedback/${interviewId}`),
  getFeedback: (interviewId) => api.get(`/feedback/${interviewId}`),
};
