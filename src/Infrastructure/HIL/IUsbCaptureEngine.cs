using System.Collections.Generic;
using System.Threading.Tasks;
using LibUsbDotNet;

namespace DeepEyeUnlocker.Infrastructure.HIL
{
    public interface IUsbCaptureEngine
    {
        /// <summary>
        /// Starts capturing USB traffic for a specific device.
        /// </summary>
        Task StartCaptureAsync(int vid, int pid, string outputPcap);

        /// <summary>
        /// Stops the current capture session.
        /// </summary>
        Task StopCaptureAsync();

        /// <summary>
        /// Parses a Pcap file into a sequence of USB packets.
        /// </summary>
        Task<IEnumerable<UsbPacket>> ParseCaptureAsync(string pcapPath);
    }
}
