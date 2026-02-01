using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace DeepEyeUnlocker.Migrations
{
    /// <inheritdoc />
    public partial class InitialDeviceSchema : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "Devices",
                columns: table => new
                {
                    ProfileId = table.Column<string>(type: "TEXT", nullable: false),
                    ModelNumber = table.Column<string>(type: "TEXT", nullable: false),
                    MarketingName = table.Column<string>(type: "TEXT", nullable: false),
                    Codename = table.Column<string>(type: "TEXT", nullable: false),
                    Brand = table.Column<string>(type: "TEXT", nullable: false),
                    Series = table.Column<string>(type: "TEXT", nullable: false),
                    Region = table.Column<string>(type: "TEXT", nullable: false),
                    Chipset_Manufacturer = table.Column<string>(type: "TEXT", nullable: false),
                    Chipset_Model = table.Column<string>(type: "TEXT", nullable: false),
                    Chipset_Platform = table.Column<string>(type: "TEXT", nullable: false),
                    Chipset_Architecture = table.Column<string>(type: "TEXT", nullable: false),
                    Chipset_SupportedUniversalMethods = table.Column<string>(type: "TEXT", nullable: false),
                    SupportedBootModes = table.Column<string>(type: "TEXT", nullable: false),
                    InterfaceClassGuids = table.Column<string>(type: "TEXT", nullable: false),
                    Security_PatchLevel = table.Column<string>(type: "TEXT", nullable: false),
                    Security_SecureBoot = table.Column<bool>(type: "INTEGER", nullable: false),
                    Security_EncryptedUserData = table.Column<bool>(type: "INTEGER", nullable: false),
                    ValidationStatus = table.Column<int>(type: "INTEGER", nullable: false),
                    KnownFirmwares = table.Column<string>(type: "TEXT", nullable: true),
                    SupportedOperations = table.Column<string>(type: "TEXT", nullable: true),
                    UsbIds = table.Column<string>(type: "TEXT", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Devices", x => x.ProfileId);
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "Devices");
        }
    }
}
