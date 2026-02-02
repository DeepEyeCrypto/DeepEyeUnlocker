using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using DeepEyeUnlocker.Core;
using DeepEyeUnlocker.Features.ModelDiscovery.Database;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Services
{
    public interface ISourceExtractor
    {
        string ToolName { get; }
        IEnumerable<string> SeedUrls { get; }
        Task<IEnumerable<SupportedModel>> ExtractAsync(string url);
    }

    public class CrawlerManager
    {
        private readonly List<ISourceExtractor> _extractors = new();
        private readonly DiscoveryDbContext _db;

        public CrawlerManager(DiscoveryDbContext db)
        {
            _db = db;
            _db.Database.EnsureCreated();
        }

        public void RegisterExtractor(ISourceExtractor extractor) => _extractors.Add(extractor);

        public async Task RunDiscoveryAsync(Action<string> logger)
        {
            foreach (var extractor in _extractors)
            {
                logger($"[CRAWLER] Starting discovery for {extractor.ToolName}...");
                foreach (var url in extractor.SeedUrls)
                {
                    try
                    {
                        var models = await extractor.ExtractAsync(url);
                        logger($"[CRAWLER] Extracted {models.Count()} models from {url}");
                        await SaveModelsAsync(models);
                    }
                    catch (Exception ex)
                    {
                        logger($"[ERROR] Failed to crawl {url}: {ex.Message}");
                    }
                }
            }
        }

        private async Task SaveModelsAsync(IEnumerable<SupportedModel> models)
        {
            foreach (var model in models)
            {
                var existing = await _db.SupportedModels.FirstOrDefaultAsync(m => 
                    m.Brand == model.Brand && 
                    m.ModelNumber == model.ModelNumber && 
                    m.Tool == model.Tool);

                if (existing == null)
                {
                    _db.SupportedModels.Add(model);
                }
                else
                {
                    // Update LastSeen and merge operations
                    existing.LastSeen = DateTime.UtcNow;
                    var existingOps = new HashSet<string>(existing.GetOperations());
                    foreach (var op in model.GetOperations()) existingOps.Add(op);
                    existing.SetOperations(existingOps);
                }
            }
            await _db.SaveChangesAsync();
        }
    }
}
