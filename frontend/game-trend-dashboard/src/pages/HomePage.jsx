import { useEffect, useState } from 'react';
import {
  getConversations,
  getLiveTrendRankings,
  getYoutubeTopGames,
} from '../api/gameTrendApi.js';
import DashboardPreview from '../components/home/DashboardPreview.jsx';
import FeatureCards from '../components/home/FeatureCards.jsx';
import Footer from '../components/home/Footer.jsx';
import HeroSection from '../components/home/HeroSection.jsx';
import StatsSection from '../components/home/StatsSection.jsx';

function HomePage({ onAsk, onNavigate, authUser, isAnalyzing = false }) {
  const [rankings, setRankings] = useState([]);
  const [topGames, setTopGames] = useState([]);
  const [conversationCount, setConversationCount] = useState(null);
  const [isLoadingDashboard, setIsLoadingDashboard] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const loadHomeDashboard = async () => {
      setIsLoadingDashboard(true);
      try {
        const [rankingData, youtubeData] = await Promise.all([
          getLiveTrendRankings({ limit: 5, sort: 'TREND_SCORE' }).catch(() => []),
          getYoutubeTopGames(5).catch(() => []),
        ]);

        if (!cancelled) {
          setRankings(Array.isArray(rankingData) ? rankingData.slice(0, 5) : []);
          setTopGames(Array.isArray(youtubeData) ? youtubeData.slice(0, 5) : []);
        }
      } finally {
        if (!cancelled) {
          setIsLoadingDashboard(false);
        }
      }
    };

    loadHomeDashboard();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    if (!authUser) {
      setConversationCount(null);
      return () => {
        cancelled = true;
      };
    }

    getConversations()
      .then((data) => {
        if (!cancelled) {
          setConversationCount(Array.isArray(data) ? data.length : 0);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setConversationCount(0);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [authUser]);

  return (
    <div className="home-page home-page-v2">
      <HeroSection onAsk={onAsk} onNavigate={onNavigate} isAnalyzing={isAnalyzing} />
      <FeatureCards onNavigate={onNavigate} />
      <StatsSection
        rankings={rankings}
        topGames={topGames}
        conversationCount={conversationCount}
        isLoading={isLoadingDashboard}
      />
      <DashboardPreview
        rankings={rankings}
        topGames={topGames}
        isLoadingRankings={isLoadingDashboard}
        onNavigate={onNavigate}
      />
      <Footer onNavigate={onNavigate} />
    </div>
  );
}

export default HomePage;
