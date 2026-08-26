export default function Footer() {
  return (
    <footer className="h-[60px] border-t border-[#1f2937] bg-[#0b1020]
    flex items-center justify-between px-6 text-sm text-gray-400">
      <p>© 2025 ChainTrack. All rights reserved.</p>

      <div className="flex gap-5">
        <button>Privacy Policy</button>
        <button>Terms of Service</button>
        <button>Contact</button>
      </div>
    </footer>
  );
}