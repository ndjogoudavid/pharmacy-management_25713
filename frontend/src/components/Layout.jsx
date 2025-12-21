import Sidebar from './Sidebar';
import Topbar from './Topbar';
import Footer from './Footer';
import './Layout.css';

export default function Layout({ children }) {
  return (
    <div className="layout">
      <Sidebar />
      <div className="layout-main">
        <Topbar />
        <main className="layout-content">{children}</main>
        <Footer />
      </div>
    </div>
  );
}
