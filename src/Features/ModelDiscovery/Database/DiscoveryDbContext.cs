using Microsoft.EntityFrameworkCore;
using DeepEyeUnlocker.Features.ModelDiscovery.Models;

namespace DeepEyeUnlocker.Features.ModelDiscovery.Database
{
    public class DiscoveryDbContext : DbContext
    {
        public DbSet<SupportedModel> SupportedModels { get; set; }

        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            string dbPath = System.IO.Path.Combine(System.AppDomain.CurrentDomain.BaseDirectory, "model_discovery.db");
            optionsBuilder.UseSqlite($"Data Source={dbPath}");
        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<SupportedModel>()
                .HasIndex(m => new { m.Brand, m.ModelNumber, m.Tool })
                .IsUnique();
        }
    }
}
