import { useCallback, useEffect, useMemo, useState } from 'react';
import Button from '../components/common/Button.jsx';
import Card from '../components/common/Card.jsx';
import LoadingIndicator from '../components/common/LoadingIndicator.jsx';
import { NaturalOnboardingResult } from '../components/onboarding/NaturalOnboardingScreen.jsx';

const projectTypeOptions = [
  { value: 'GAME_IDEA', label: '게임 아이디어' },
  { value: 'MARKET_ANALYSIS', label: '시장 분석' },
  { value: 'REINTERPRETATION', label: '과거 게임 재해석' },
  { value: 'PERSONAL_RECOMMENDATION', label: '개인 추천' },
  { value: 'STREAMING_TREND', label: '방송 트렌드' },
];

const featureOptions = [
  { value: 'webcam', label: 'Webcam' },
  { value: 'tts', label: 'TTS' },
  { value: 'stt', label: 'STT' },
];

const initialProjectForm = {
  title: '',
  description: '',
  projectType: 'GAME_IDEA',
  targetAudience: '',
  preferredPlatform: 'PC',
  interactionFeatures: [],
};

function ProjectsPage({
  dashboard,
  authUser,
  isAuthLoading,
  projectId,
  onGoLogin,
  onGoRegister,
  onBackToProjects,
  onOpenProject,
}) {
  if (isAuthLoading) {
    return <LoadingIndicator label="로그인 상태를 확인하는 중" />;
  }

  if (!authUser) {
    return <ProjectLoginPrompt onGoLogin={onGoLogin} onGoRegister={onGoRegister} />;
  }

  if (projectId) {
    return (
      <ProjectDetail
        dashboard={dashboard}
        projectId={projectId}
        onBackToProjects={onBackToProjects}
      />
    );
  }

  return (
    <ProjectList
      dashboard={dashboard}
      onOpenProject={onOpenProject}
    />
  );
}

function ProjectLoginPrompt({ onGoLogin, onGoRegister }) {
  return (
    <Card className="project-auth-card">
      <p className="section-kicker">Projects</p>
      <h2>내 프로젝트는 로그인 후 사용할 수 있어요</h2>
      <p>
        로그인하면 분석 결과를 프로젝트에 저장하고, 나중에 같은 맥락으로 이어서 볼 수 있습니다.
      </p>
      <div className="auth-actions">
        <Button onClick={onGoLogin}>로그인하기</Button>
        <Button variant="secondary" onClick={onGoRegister}>회원가입하기</Button>
      </div>
    </Card>
  );
}

function ProjectList({ dashboard, onOpenProject }) {
  const [showForm, setShowForm] = useState(false);

  useEffect(() => {
    dashboard.loadProjects();
  }, [dashboard.loadProjects]);

  const activeProjects = useMemo(
    () => dashboard.projects.filter((project) => project.status !== 'ARCHIVED'),
    [dashboard.projects]
  );

  const handleCreate = async (payload) => {
    const project = await dashboard.createUserProject(payload);
    setShowForm(false);
    onOpenProject?.(project.id);
  };

  return (
    <div className="projects-page">
      <Card className="projects-hero-card">
        <div>
          <p className="section-kicker">My Projects</p>
          <h2>내 프로젝트</h2>
          <p>게임 아이디어, 시장 분석, 재해석 후보를 프로젝트별로 저장하고 이어서 분석합니다.</p>
        </div>
        <Button onClick={() => setShowForm((visible) => !visible)}>
          {showForm ? '목록으로 돌아가기' : '새 프로젝트 만들기'}
        </Button>
      </Card>

      {showForm && (
        <ProjectForm
          onSubmit={handleCreate}
          isSubmitting={dashboard.isCreatingProject}
        />
      )}

      {dashboard.isLoadingProjects && <LoadingIndicator label="프로젝트를 불러오는 중" />}

      {!dashboard.isLoadingProjects && activeProjects.length === 0 && !showForm && (
        <Card className="project-empty-card">
          <h3>아직 프로젝트가 없습니다</h3>
          <p>새 프로젝트를 만들고 Agent 분석 결과를 연결해보세요.</p>
        </Card>
      )}

      {activeProjects.length > 0 && (
        <div className="project-card-grid">
          {activeProjects.map((project) => (
            <ProjectCard
              project={project}
              key={project.id}
              onOpen={() => onOpenProject?.(project.id)}
              onSelect={() => dashboard.selectProject(project.id)}
              onArchive={() => dashboard.archiveUserProject(project.id).catch(() => {})}
              isDeleting={dashboard.isDeletingProject}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function ProjectForm({
  onSubmit,
  isSubmitting,
  initialValue = initialProjectForm,
  submitLabel = '프로젝트 생성',
  resetAfterSubmit = true,
}) {
  const [form, setForm] = useState(() => toProjectFormValue(initialValue));

  useEffect(() => {
    setForm(toProjectFormValue(initialValue));
  }, [initialValue]);

  const updateField = (field, value) => {
    setForm((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const toggleFeature = (feature) => {
    setForm((prev) => {
      const selected = prev.interactionFeatures.includes(feature);
      return {
        ...prev,
        interactionFeatures: selected
          ? prev.interactionFeatures.filter((item) => item !== feature)
          : [...prev.interactionFeatures, feature],
      };
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      await onSubmit({
        ...form,
        interactionFeatures: form.interactionFeatures,
      });
      if (resetAfterSubmit) {
        setForm(initialProjectForm);
      }
    } catch {
      // 호출 쪽에서 전역 오류 메시지를 이미 설정합니다.
    }
  };

  return (
    <Card className="project-form-card">
      <form className="project-form" onSubmit={handleSubmit}>
        <label>
          <span>프로젝트 제목</span>
          <input
            value={form.title}
            onChange={(event) => updateField('title', event.target.value)}
            required
          />
        </label>
        <label>
          <span>설명</span>
          <textarea
            value={form.description}
            onChange={(event) => updateField('description', event.target.value)}
            rows="4"
          />
        </label>
        <div className="project-form-grid">
          <label>
            <span>프로젝트 타입</span>
            <select
              value={form.projectType}
              onChange={(event) => updateField('projectType', event.target.value)}
            >
              {projectTypeOptions.map((option) => (
                <option value={option.value} key={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <label>
            <span>타깃 사용자</span>
            <input
              value={form.targetAudience}
              onChange={(event) => updateField('targetAudience', event.target.value)}
              placeholder="예: 1인 개발자, 스트리머, 친구와 플레이하는 유저"
            />
          </label>
          <label>
            <span>선호 플랫폼</span>
            <input
              value={form.preferredPlatform}
              onChange={(event) => updateField('preferredPlatform', event.target.value)}
            />
          </label>
        </div>
        <fieldset className="project-feature-fieldset">
          <legend>상호작용 기능</legend>
          <div className="feature-toggle-group">
            {featureOptions.map((feature) => (
              <button
                className={`feature-toggle ${form.interactionFeatures.includes(feature.value) ? 'selected' : ''}`}
                type="button"
                key={feature.value}
                onClick={() => toggleFeature(feature.value)}
              >
                {feature.label}
              </button>
            ))}
          </div>
        </fieldset>
        <div className="auth-actions">
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? '저장 중' : submitLabel}
          </Button>
        </div>
      </form>
    </Card>
  );
}

function ProjectCard({ project, onOpen, onSelect, onArchive, isDeleting }) {
  return (
    <article className="project-card">
      <div className="project-card-top">
        <div>
          <span>{formatProjectType(project.projectType)}</span>
          <h3>{project.title}</h3>
        </div>
        <strong>{formatDateTime(project.updatedAt)}</strong>
      </div>
      {project.description && <p>{project.description}</p>}
      <div className="project-meta-grid">
        <MetaItem label="타깃" value={project.targetAudience} />
        <MetaItem label="플랫폼" value={project.preferredPlatform} />
        <MetaItem label="상호작용" value={formatFeatures(project.interactionFeatures)} />
      </div>
      <div className="project-card-actions">
        <Button onClick={onOpen}>상세 보기</Button>
        <Button variant="secondary" onClick={onSelect}>Agent에서 선택</Button>
        <Button variant="secondary" onClick={onArchive} disabled={isDeleting}>보관</Button>
      </div>
    </article>
  );
}

function ProjectDetail({ dashboard, projectId, onBackToProjects }) {
  const [project, setProject] = useState(null);
  const [histories, setHistories] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingHistories, setIsLoadingHistories] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  const {
    analyzeFollowUpQuestion,
    analyzeNaturalOnboarding,
    clearError,
    clearSuccess,
    isAnalyzingFollowUp,
    isAnalyzingOnboarding,
    isUpdatingProject,
    loadProjectDetail,
    loadProjectHistories,
    onboardingData,
    onboardingResult,
    prepareFollowUpQuestion,
    selectProject,
    startNewAgentQuestion,
    updateOnboardingField,
  } = dashboard;

  const reloadHistories = useCallback(async () => {
    setIsLoadingHistories(true);
    try {
      const data = await loadProjectHistories(projectId);
      setHistories(data);
    } catch {
      setHistories([]);
    } finally {
      setIsLoadingHistories(false);
    }
  }, [loadProjectHistories, projectId]);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    selectProject(projectId);
    loadProjectDetail(projectId)
      .then((data) => {
        if (!cancelled) {
          setProject(data);
        }
      })
      .catch((error) => {
        clearError();
        clearSuccess();
        console.warn(error);
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [clearError, clearSuccess, loadProjectDetail, projectId, selectProject]);

  useEffect(() => {
    reloadHistories();
  }, [reloadHistories]);

  const handleAnalyze = async () => {
    await analyzeNaturalOnboarding(projectId);
    await reloadHistories();
  };

  const handleUpdate = async (payload) => {
    const updatedProject = await dashboard.updateUserProject(projectId, payload);
    setProject(updatedProject);
    setIsEditing(false);
  };

  const handleFollowUp = async (question, parentHistoryId = onboardingResult?.historyId) => {
    await analyzeFollowUpQuestion(question, parentHistoryId, projectId);
    await reloadHistories();
  };

  if (isLoading && !project) {
    return <LoadingIndicator label="프로젝트를 불러오는 중" />;
  }

  if (!project) {
    return (
      <Card className="project-empty-card">
        <h3>프로젝트를 찾을 수 없습니다</h3>
        <Button onClick={onBackToProjects}>목록으로 돌아가기</Button>
      </Card>
    );
  }

  return (
    <div className="project-detail-page">
      <Card className="project-detail-hero">
        <div>
          <p className="section-kicker">Project</p>
          <h2>{project.title}</h2>
          {project.description && <p>{project.description}</p>}
          <div className="project-meta-grid">
            <MetaItem label="타입" value={formatProjectType(project.projectType)} />
            <MetaItem label="타깃" value={project.targetAudience} />
            <MetaItem label="플랫폼" value={project.preferredPlatform} />
            <MetaItem label="기능" value={formatFeatures(project.interactionFeatures)} />
          </div>
        </div>
        <div className="project-card-actions">
          <Button variant="secondary" onClick={() => setIsEditing((editing) => !editing)}>
            {isEditing ? '수정 닫기' : '프로젝트 수정'}
          </Button>
          <Button variant="secondary" onClick={onBackToProjects}>목록으로</Button>
        </div>
      </Card>

      {isEditing && (
        <ProjectForm
          initialValue={project}
          onSubmit={handleUpdate}
          isSubmitting={isUpdatingProject}
          submitLabel="프로젝트 수정"
          resetAfterSubmit={false}
        />
      )}

      <Card className="project-agent-card">
        <p className="section-kicker">Agent</p>
        <h3>이 프로젝트에 분석 저장하기</h3>
        <label className="agent-message-field">
          <span>질문</span>
          <textarea
            value={onboardingData.message}
            onChange={(event) => updateOnboardingField('message', event.target.value)}
            placeholder="이 프로젝트 관점에서 개발 가능성이나 재해석 방향을 물어보세요."
            rows="6"
          />
        </label>
        <div className="agent-actions">
          <Button variant="secondary" onClick={startNewAgentQuestion}>
            새 질문 시작
          </Button>
          <Button
            onClick={handleAnalyze}
            disabled={isAnalyzingOnboarding || !onboardingData.message.trim()}
          >
            {isAnalyzingOnboarding ? '분석 중' : '프로젝트에 분석 저장'}
          </Button>
        </div>
      </Card>

      {onboardingResult?.savedToProject
        && Number(onboardingResult.projectId) === Number(projectId)
        && (
          <NaturalOnboardingResult
            result={onboardingResult}
            onUseQuestion={handleFollowUp}
            onPrepareQuestion={prepareFollowUpQuestion}
            isAnalyzingFollowUp={isAnalyzingFollowUp}
            analyzingFollowUpQuestion={dashboard.analyzingFollowUpQuestion}
          />
        )}

      <Card className="project-history-card">
        <div className="card-heading">
          <div>
            <p className="section-kicker">Histories</p>
            <h3>연결된 분석 기록</h3>
          </div>
          <Button variant="secondary" onClick={reloadHistories} disabled={isLoadingHistories}>
            {isLoadingHistories ? '불러오는 중' : '새로고침'}
          </Button>
        </div>

        {isLoadingHistories && <LoadingIndicator label="프로젝트 분석 기록을 불러오는 중" />}
        {!isLoadingHistories && histories.length === 0 && (
          <p className="empty-state">아직 이 프로젝트에 연결된 분석 기록이 없습니다.</p>
        )}
        {!isLoadingHistories && histories.length > 0 && (
          <div className="project-history-list">
            {histories.map((history) => (
              <article className="project-history-item" key={history.id}>
                <time dateTime={history.createdAt}>{formatDateTime(history.createdAt)}</time>
                <h4>{history.message}</h4>
                <p>{history.summary}</p>
                <span>history #{history.id}</span>
              </article>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}

function MetaItem({ label, value }) {
  return (
    <div className="project-meta-item">
      <span>{label}</span>
      <strong>{value || '미정'}</strong>
    </div>
  );
}

function formatProjectType(projectType) {
  return projectTypeOptions.find((option) => option.value === projectType)?.label || projectType || '프로젝트';
}

function toProjectFormValue(project = initialProjectForm) {
  return {
    title: project.title || '',
    description: project.description || '',
    projectType: project.projectType || 'GAME_IDEA',
    targetAudience: project.targetAudience || '',
    preferredPlatform: project.preferredPlatform || 'PC',
    interactionFeatures: Array.isArray(project.interactionFeatures)
      ? project.interactionFeatures
      : [],
  };
}

function formatFeatures(features) {
  if (!Array.isArray(features) || features.length === 0) {
    return '없음';
  }
  return features.map((feature) => String(feature).toUpperCase()).join(', ');
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

export default ProjectsPage;
