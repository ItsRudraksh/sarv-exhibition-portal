import { AdminApp } from './features/admin/AdminApp'
import { InquiryApp } from './features/inquiry/InquiryApp'
import { StaffApp } from './features/staff/StaffApp'
import './styles/tokens.css'
import './styles/app.css'
import './styles/staff.css'

function App() {
  const path = window.location.pathname
  if (path.startsWith('/admin')) {
    return <AdminApp />
  }
  if (path.startsWith('/staff')) {
    return <StaffApp />
  }
  return <InquiryApp />
}

export default App
