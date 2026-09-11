/**
 * Path switch: /admin AdminApp, /staff StaffApp, otherwise visitor InquiryApp. Not React Router.
 * stripPublicBase keeps this working when the SPA is mounted at /exhibit.
 */
import { AdminApp } from './features/admin/AdminApp'
import { InquiryApp } from './features/inquiry/InquiryApp'
import { StaffApp } from './features/staff/StaffApp'
import { stripPublicBase } from './lib/publicPath'
import './styles/tokens.css'
import './styles/app.css'
import './styles/staff.css'

function App() {
  const path = stripPublicBase(window.location.pathname)
  if (path.startsWith('/admin')) {
    return <AdminApp />
  }
  if (path.startsWith('/staff')) {
    return <StaffApp />
  }
  return <InquiryApp />
}

export default App
