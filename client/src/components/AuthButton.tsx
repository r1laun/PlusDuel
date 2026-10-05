import { useEffect, useState } from 'react';
import type { Session } from '@supabase/supabase-js';
import { isAuthConfigured, supabase } from '../auth/supabase';
import { playClick } from '../sound/click';

interface Props {
  session: Session | null;
  compact?: boolean;
}

export default function AuthButton({ session, compact }: Props) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  // Clear the error on the next successful session change.
  useEffect(() => {
    if (session) setError('');
  }, [session]);

  if (!isAuthConfigured()) return null;

  const signIn = async () => {
    const sb = supabase();
    if (!sb) return;
    playClick();
    setBusy(true);
    setError('');
    const { error } = await sb.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: window.location.origin },
    });
    setBusy(false);
    if (error) setError('Sign-in failed — try again.');
  };

  const signOut = async () => {
    const sb = supabase();
    if (!sb) return;
    playClick();
    setBusy(true);
    await sb.auth.signOut();
    setBusy(false);
  };

  if (session) {
    const label = session.user.user_metadata?.full_name
      ?? session.user.email
      ?? 'Player';
    return (
      <>
        <div className="pd-field">
          <span className="pd-label" title={session.user.email ?? ''}>
            {String(label).slice(0, 20)}
          </span>
          <button className="pd-btn pd-btn--outline pd-btn--sm" onClick={signOut} disabled={busy}>
            Sign out
          </button>
        </div>
        {error && <div className="pd-status">{error}</div>}
      </>
    );
  }

  return (
    <>
      <button
        className={`pd-btn pd-btn--outline${compact ? ' pd-btn--sm' : ''}`}
        onClick={signIn}
        disabled={busy}
      >
        {busy ? 'Opening Google…' : compact ? 'Sign in' : 'Sign in with Google'}
      </button>
      {error && <div className="pd-status">{error}</div>}
    </>
  );
}
