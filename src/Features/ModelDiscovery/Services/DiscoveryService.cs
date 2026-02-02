using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using DeepEyeUnlocker.Features.ModelDiscovery.Database;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;
using Microsoft.EntityFrameworkCore;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Services
{
    public class DiscoveryService
    {
        private readonly DiscoveryDbContext _db;

        public DiscoveryService(DiscoveryDbContext db)
        {
            _db = db;
        }

        public async Task<List<string>> GetBrandsAsync()
        {
            return await _db.SupportedModels
                .Select(m => m.Brand)
                .Distinct()
                .OrderBy(b => b)
                .ToListAsync();
        }

        public async Task<List<SupportedModel>> GetModelsAsync(string? brand = null, string? search = null, int limit = 500, int offset = 0)
        {
            var query = _db.SupportedModels.AsQueryable();

            if (!string.IsNullOrEmpty(brand))
            {
                query = query.Where(m => m.Brand == brand);
            }

            if (!string.IsNullOrEmpty(search))
            {
                string searchLower = search.ToLower();
                query = query.Where(m => m.MarketingName.ToLower().Contains(searchLower) || 
                                         (m.ModelNumber != null && m.ModelNumber.ToLower().Contains(searchLower)) ||
                                         (m.Codename != null && m.Codename.ToLower().Contains(searchLower)));
            }

            return await query
                .OrderBy(m => m.Brand)
                .ThenBy(m => m.MarketingName)
                .Skip(offset)
                .Take(limit)
                .ToListAsync();
        }

        public async Task ExportToCsvAsync(string filePath, string? brand = null)
        {
            var query = _db.SupportedModels.AsQueryable();
            if (!string.IsNullOrEmpty(brand))
            {
                query = query.Where(m => m.Brand == brand);
            }

            var models = await query.ToListAsync();
            var csv = new StringBuilder();
            csv.AppendLine("Tool,Brand,MarketingName,ModelNumber,Codename,Chipset,Operations,Modes,Source");

            foreach (var m in models)
            {
                csv.AppendLine($"\"{m.Tool}\",\"{m.Brand}\",\"{m.MarketingName}\",\"{m.ModelNumber}\",\"{m.Codename}\",\"{m.ChipsetFamily} {m.ChipsetModel}\",\"{m.OperationsJson.Replace("\"", "\"\"")}\",\"{m.ModesJson.Replace("\"", "\"\"")}\",\"{m.SourceUrl}\"");
            }

            await File.WriteAllTextAsync(filePath, csv.ToString());
        }
    }
}
