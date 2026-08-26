import {
  LayoutDashboard,
  Truck,
  ArrowLeftRight,
  Boxes,
  BarChart3,
  ShieldCheck,
} from "lucide-react";

const menus = [
  {
    title: "Dashboard",
    icon: LayoutDashboard,
    active: true,
  },
  {
    title: "Shipments",
    icon: Truck,
  },
  {
    title: "Transactions",
    icon: ArrowLeftRight,
  },
  {
    title: "Inventory",
    icon: Boxes,
  },
  {
    title: "Analytics",
    icon: BarChart3,
  },
  {
    title: "Certificates",
    icon: ShieldCheck,
  },
];

export default function Sidebar() {
  return (
    <aside className="w-[250px] min-h-screen bg-[#0b1020] border-r border-[#1f2937] flex flex-col justify-between">
      <div>
        {/* Logo */}
        <div className="p-6 border-b border-[#1f2937]">
          <h1 className="text-2xl font-bold text-blue-500">
            ChainTrack
          </h1>

          <p className="text-gray-400 text-sm mt-1">
            Enterprise Verified
          </p>
        </div>

        {/* Menu */}
        <div className="p-4 space-y-2">
          {menus.map((menu, index) => {
            const Icon = menu.icon;

            return (
              <button
                key={index}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all
                ${
                  menu.active
                    ? "bg-blue-600 text-white"
                    : "text-gray-400 hover:bg-[#141b2d] hover:text-white"
                }`}
              >
                <Icon size={18} />
                <span>{menu.title}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* User */}
      <div className="p-4 border-t border-[#1f2937]">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-full bg-yellow-100 flex items-center justify-center text-black font-bold">
            A
          </div>

          <div>
            <h3 className="font-semibold">Alex Rivera</h3>
            <p className="text-sm text-gray-400">
              Logistics Lead
            </p>
          </div>
        </div>
      </div>
    </aside>
  );
}