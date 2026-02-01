using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DeepEyeUnlocker.Core.Architecture;
using DeepEyeUnlocker.Core.Models;
using DeepEyeUnlocker.Protocols.ModelSpecific;
using DeepEyeUnlocker.Protocols.MTK;
using DeepEyeUnlocker.Protocols.Qualcomm;
using DeepEyeUnlocker.Protocols.SPD;

namespace DeepEyeUnlocker.Core.Services
{
    public class HybridOperationRouter
    {
        private readonly DeviceClassifier _classifier;
        private readonly ModelSpecificPlugin _modelSpecificPlugin;
        private readonly IEnumerable<IUniversalPlugin> _universalPlugins;
        private readonly IUserInteraction _userInteraction;

        public HybridOperationRouter(
            DeviceClassifier classifier,
            ModelSpecificPlugin modelSpecificPlugin,
            IEnumerable<IUniversalPlugin> universalPlugins,
            IUserInteraction userInteraction)
        {
            _classifier = classifier;
            _modelSpecificPlugin = modelSpecificPlugin;
            _universalPlugins = universalPlugins;
            _userInteraction = userInteraction;
        }

        public async Task<OperationResult> ExecuteSmartAsync(
            string operation,
            DeviceProfile device,
            Dictionary<string, object> parameters)
        {
            // 1. Classify & Recommend
            var category = _classifier.Classify(device);
            var strategy = _classifier.GetRecommendedStrategy(category);

            // 2. Routing Logic
            
            // PATH A: High-Security / Flagship -> FORCE Model Specific
            if (strategy.Type == StrategyType.Model_Specific)
            {
                return await _modelSpecificPlugin.ExecuteOperationAsync(operation, device, parameters);
            }

            // PATH B: Legacy / Universal -> FORCE Universal Plugin
            if (strategy.Type == StrategyType.Miracle_Universal)
            {
                // Warn if risk is high
                if (strategy.Risk == RiskLevel.High)
                {
                    if (!await _userInteraction.ConfirmAsync("High Risk Operation", strategy.Reason, true))
                    {
                        return new OperationResult { Success = false, Message = "User Cancelled High Risk Operation" };
                    }
                }
                return await ExecuteUniversalAsync(operation, device, parameters);
            }

            // PATH C: Hybrid / Fallback
            // Try Model-Specific first
            var result = await _modelSpecificPlugin.ExecuteOperationAsync(operation, device, parameters);
            
            if (result.Success) return result;

            // If failed (and safe to retry), try Universal
            if (!result.Success && strategy.Type != StrategyType.Universal_Fallback)
            {
                 bool userApprovedRisk = await _userInteraction.ConfirmAsync(
                     "Fallback Required", 
                     $"Model-specific method failed. {strategy.Reason}\nDo you want to try the Universal method? This carries higher risk.", 
                     true);
                 
                 if (userApprovedRisk)
                 {
                     return await ExecuteUniversalAsync(operation, device, parameters);
                 }
            }

            return result;
        }

        private async Task<OperationResult> ExecuteUniversalAsync(
            string operation,
            DeviceProfile device,
            Dictionary<string, object> parameters)
        {
            IUniversalPlugin plugin = GetUniversalPluginForChipset(device.Chipset);
            
            if (plugin == null)
                return new OperationResult { Success = false, Message = "No Universal Plugin for this Chipset" };

            bool isKeypad = device.Brand == "Nokia" || device.Brand == "Jio"; 

            if (isKeypad)
            {
                return await plugin.ExecuteKeypadOperationAsync(operation, device);
            }
            
            return await plugin.ExecuteOperationAsync(operation, parameters, device);
        }

        private IUniversalPlugin GetUniversalPluginForChipset(ChipsetInfo chipset)
        {
            var model = chipset.Model.ToUpper();

            // Simple pattern matching based on ProtocolName or hardcoded logic for now
            // In future, use IProtocolPlugin.SupportedChips with pattern matcher
            
            foreach (var plugin in _universalPlugins)
            {
                if ((model.Contains("MT") || model.Contains("DIMENSITY") || model.Contains("HELIO")) 
                    && plugin.ProtocolName.Contains("MTK"))
                    return plugin;

                if ((model.Contains("SC") || model.Contains("SPD") || model.Contains("UNISOC"))
                    && plugin.ProtocolName.Contains("SPD"))
                    return plugin;

                if ((model.Contains("SNAPDRAGON") || model.Contains("MSM") || model.Contains("SDM"))
                    && plugin.ProtocolName.Contains("Qualcomm"))
                    return plugin;
            }

            return null;
        }
    }
}
