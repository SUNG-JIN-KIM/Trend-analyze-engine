export function getScoreTone(score) {
  if (score >= 80) {
    return 'excellent';
  }
  if (score >= 60) {
    return 'good';
  }
  if (score >= 40) {
    return 'fair';
  }
  return 'low';
}

export function formatScore(score) {
  if (score === null || score === undefined || Number.isNaN(Number(score))) {
    return '-';
  }
  return Number(score).toFixed(1);
}
