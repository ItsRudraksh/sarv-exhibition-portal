import { InquiryApp } from './features/inquiry/InquiryApp'
import { StaffApp } from './features/staff/StaffApp'
import './styles/tokens.css'
import './styles/app.css'
import './styles/staff.css'

function App() {
  if (window.location.pathname.startsWith('/staff')) {
    return <StaffApp />
  }
  return <InquiryApp />
}

export default App
