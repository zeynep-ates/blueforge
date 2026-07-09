import { Route, Routes } from 'react-router-dom'
import { NewIdeaPage } from '@/pages/NewIdeaPage'
import { ProjectDetailPage } from '@/pages/ProjectDetailPage'
import { ProjectsListPage } from '@/pages/ProjectsListPage'
import { VersionDiffPage } from '@/pages/VersionDiffPage'
import { WorkspacePage } from '@/pages/WorkspacePage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<NewIdeaPage />} />
      <Route path="/projects" element={<ProjectsListPage />} />
      <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
      <Route path="/projects/:projectId/versions/:versionNumber" element={<WorkspacePage />} />
      <Route path="/projects/:projectId/diff/:fromVersion/:toVersion" element={<VersionDiffPage />} />
    </Routes>
  )
}

export default App
