import { Route, Routes } from 'react-router-dom'
import { NewIdeaPage } from '@/pages/NewIdeaPage'
import { WorkspacePage } from '@/pages/WorkspacePage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<NewIdeaPage />} />
      <Route path="/projects/:projectId/versions/:versionNumber" element={<WorkspacePage />} />
    </Routes>
  )
}

export default App
