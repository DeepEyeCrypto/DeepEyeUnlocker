import { Smartphone, Battery, HardDrive, ShieldCheck, Cpu, Info } from "lucide-react";

interface DeviceInfo {
  serial: String;
  model: String;
  brand: String;
  android_version: String;
  sdk_int: String;
  build_id: String;
  security_patch: String;
  bootloader_status: String;
  root_status: boolean;
  frp_status: String;
  battery_level: String;
  imei: String;
  storage: String;
}

interface DeviceInfoDashboardProps {
  info: DeviceInfo;
}

export const DeviceInfoDashboard = ({ info }: DeviceInfoDashboardProps) => {
  return (
    <div className="flex flex-col gap-6 p-4">
      {/* Header Info */}
      <div className="flex items-center gap-4 p-4 bg-white/5 border border-white/10 rounded-2xl backdrop-blur-xl">
        <div className="p-3 bg-indigo-500/20 rounded-xl">
          <Smartphone className="w-8 h-8 text-indigo-400" />
        </div>
        <div>
          <h2 className="text-xl font-bold text-white">{info.brand} {info.model}</h2>
          <p className="text-sm text-gray-400">SN: {info.serial}</p>
        </div>
        <div className="ml-auto flex gap-2">
            <span className={`px-3 py-1 rounded-full text-xs font-medium ${info.root_status ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'bg-green-500/20 text-green-400 border border-green-500/30'}`}>
                {info.root_status ? 'ROOTED' : 'UNROOTED'}
            </span>
            <span className="px-3 py-1 bg-white/5 border border-white/10 rounded-full text-xs font-medium text-gray-300">
                Android {info.android_version}
            </span>
        </div>
      </div>

      {/* Grid Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <InfoCard 
          icon={<Battery className="w-5 h-5 text-green-400" />}
          label="Battery Level"
          value={info.battery_level}
        />
        <InfoCard 
          icon={<HardDrive className="w-5 h-5 text-blue-400" />}
          label="Internal Storage"
          value={info.storage}
        />
        <InfoCard 
          icon={<ShieldCheck className="w-5 h-5 text-amber-400" />}
          label="Security Patch"
          value={info.security_patch}
        />
        <InfoCard 
          icon={<Cpu className="w-5 h-5 text-purple-400" />}
          label="Bootloader"
          value={info.bootloader_status}
        />
        <InfoCard 
          icon={<Info className="w-5 h-5 text-gray-400" />}
          label="IMEI"
          value={info.imei}
        />
         <InfoCard 
          icon={<Info className="w-5 h-5 text-gray-400" />}
          label="Build ID"
          value={info.build_id}
        />
      </div>
      
      {/* Detailed Table */}
      <div className="bg-white/5 border border-white/10 rounded-2xl overflow-hidden backdrop-blur-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-white/5 text-gray-400 uppercase text-[10px] tracking-wider font-semibold">
            <tr>
              <th className="px-4 py-3">Property</th>
              <th className="px-4 py-3">Value</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/10">
            <TableRow label="Manufacturer" value={info.brand} />
            <TableRow label="SDK Level" value={info.sdk_int} />
            <TableRow label="FRP Partition" value={info.frp_status} />
          </tbody>
        </table>
      </div>
    </div>
  );
};

const InfoCard = ({ icon, label, value }: { icon: React.ReactNode, label: string, value: any }) => (
  <div className="p-4 bg-white/5 border border-white/10 rounded-2xl hover:bg-white/10 transition-colors">
    <div className="flex items-center gap-3 mb-2">
      {icon}
      <span className="text-xs text-gray-400 uppercase font-bold tracking-wider">{label}</span>
    </div>
    <div className="text-lg font-medium text-white truncate">{value}</div>
  </div>
);

const TableRow = ({ label, value }: { label: string, value: any }) => (
  <tr className="hover:bg-white/5 transition-colors">
    <td className="px-4 py-3 text-gray-400 font-medium">{label}</td>
    <td className="px-4 py-3 text-gray-100">{value}</td>
  </tr>
);
