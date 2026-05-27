import { useEffect } from 'react';

function OAuthCallbackPage({ onOAuthToken, onOAuthSession, onGoHome, onGoLogin }) {
  useEffect(() => {
    let cancelled = false;
    const query = new URLSearchParams(window.location.search);
    const token = query.get('token') || query.get('accessToken');
    const error = query.get('error');

    const finish = async () => {
      try {
        if (error) {
          onGoLogin();
          return;
        }

        if (token) {
          await onOAuthToken(token);
        } else {
          await onOAuthSession();
        }

        if (!cancelled) {
          onGoHome();
        }
      } catch {
        if (!cancelled) {
          onGoLogin();
        }
      }
    };

    finish();

    return () => {
      cancelled = true;
    };
  }, [onGoHome, onGoLogin, onOAuthSession, onOAuthToken]);

  return null;
}

export default OAuthCallbackPage;
