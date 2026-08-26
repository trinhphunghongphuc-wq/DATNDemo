import { Layout } from "@/components/Layout";
import { Construction } from "lucide-react";

interface PlaceholderProps {
  title: string;
}

export default function Placeholder({ title }: PlaceholderProps) {
  return (
    <Layout>
      <header className="hidden lg:flex items-center gap-4 px-6 py-4 border-b border-white/[0.06] flex-shrink-0">
        <h1 className="text-white font-bold text-xl">{title}</h1>
      </header>
      <main className="flex-1 flex items-center justify-center p-8">
        <div className="text-center max-w-sm">
          <div className="w-16 h-16 rounded-2xl bg-blue-500/10 flex items-center justify-center mx-auto mb-5">
            <Construction size={28} className="text-blue-400" />
          </div>
          <h2 className="text-white font-semibold text-xl mb-2">{title}</h2>
          <p className="text-slate-400 text-sm leading-relaxed">
            This section is under construction. Continue prompting to have this
            page filled in with real content.
          </p>
        </div>
      </main>
    </Layout>
  );
}
