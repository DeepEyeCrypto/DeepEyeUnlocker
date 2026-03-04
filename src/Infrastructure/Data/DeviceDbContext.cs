using Microsoft.EntityFrameworkCore;
using DeepEyeUnlocker.Core.Models;
using System;

namespace DeepEyeUnlocker.Infrastructure.Data
{
    public class DeviceDbContext : DbContext
    {
        public DbSet<DeviceProfile> Devices { get; set; }

        public string DbPath { get; private set; }

        public DeviceDbContext()
        {
            // Default path for local development
            var folder = Environment.SpecialFolder.LocalApplicationData;
            var path = Environment.GetFolderPath(folder);
            DbPath = System.IO.Path.Join(path, "deepeye_profiles_v3.db");
        }

        // Constructor for DI/Services to override path
        public DeviceDbContext(DbContextOptions<DeviceDbContext> options) : base(options)
        {
            DbPath = string.Empty; // Initialized via options or later override
        }

        protected override void OnConfiguring(DbContextOptionsBuilder options)
        {
            if (!options.IsConfigured)
            {
                options.UseSqlite($"Data Source={DbPath}");
            }
        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            // Map complex types as owned entities (JSON columns in SQLite/Postgres)
            modelBuilder.Entity<DeviceProfile>()
                .OwnsOne(d => d.Chipset);

            modelBuilder.Entity<DeviceProfile>()
                .OwnsOne(d => d.Security);

            modelBuilder.Entity<DeviceProfile>()
                .OwnsOne(d => d.FrpInfo);

            modelBuilder.Entity<DeviceProfile>()
                .OwnsOne(d => d.ServiceModes, sm =>
                {
                    sm.OwnsOne(s => s.Preloader, pl =>
                    {
                        pl.OwnsOne(p => p.TestPoint);
                    });
                });

            modelBuilder.Entity<DeviceProfile>()
                .OwnsOne(d => d.Loaders);

            modelBuilder.Entity<DeviceProfile>()
                .OwnsMany(d => d.UsbIds, b => b.ToJson());

            modelBuilder.Entity<DeviceProfile>()
                .OwnsMany(d => d.SupportedOperations, b => b.ToJson());

            modelBuilder.Entity<DeviceProfile>()
                .OwnsMany(d => d.KnownFirmwares, b => b.ToJson());
        }
    }
}
