using System.Threading.Tasks;

namespace DeepEyeUnlocker.Core.Services
{
    public interface IUserInteraction
    {
        Task<bool> ConfirmAsync(string title, string message, bool isRisk);
    }
}
