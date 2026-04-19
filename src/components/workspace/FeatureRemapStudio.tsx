import { useMemo, useState } from "react";
import {
  BRAND_OPTIONS,
  FEATURE_REMAPPINGS,
  FEATURE_SUMMARY,
  WORKSPACE_META,
  type RemappedFeature,
  type RiskLevel,
  type WorkspaceId,
} from "../../lib/desktopWorkspace";
import "./FeatureRemapStudio.css";

type BrandSection = {
  brand: string;
  groups: Array<{
    id: string;
    title: string;
    features: RemappedFeature[];
  }>;
  count: number;
};

type FeatureRemapStudioProps = {
  onOpenWorkspace: (workspaceId: WorkspaceId, feature: RemappedFeature) => void;
};

function riskTone(risk: RiskLevel): "safe" | "moderate" | "high" | "critical" {
  switch (risk) {
    case "MODERATE":
      return "moderate";
    case "HIGH":
      return "high";
    case "CRITICAL":
      return "critical";
    default:
      return "safe";
  }
}

function riskLabel(risk: RiskLevel): string {
  return risk.charAt(0) + risk.slice(1).toLowerCase();
}

function featureMatchesQuery(feature: RemappedFeature, query: string): boolean {
  const normalizedQuery = query.trim().toLowerCase();
  if (!normalizedQuery) {
    return true;
  }

  return [
    feature.brand,
    feature.groupTitle,
    feature.label,
    feature.description,
    feature.id,
    feature.workspaceLabel,
    feature.commandHint ?? "",
    feature.modes.join(" "),
    feature.chipsets.join(" "),
  ]
    .join(" ")
    .toLowerCase()
    .includes(normalizedQuery);
}

function groupByBrand(features: RemappedFeature[]): BrandSection[] {
  const brandMap = new Map<string, Map<string, BrandSection["groups"][number]>>();

  for (const feature of features) {
    if (!brandMap.has(feature.brand)) {
      brandMap.set(feature.brand, new Map());
    }

    const groups = brandMap.get(feature.brand);
    if (!groups) {
      continue;
    }

    if (!groups.has(feature.groupId)) {
      groups.set(feature.groupId, {
        id: feature.groupId,
        title: feature.groupTitle,
        features: [],
      });
    }

    const group = groups.get(feature.groupId);
    if (group) {
      group.features.push(feature);
    }
  }

  return [...brandMap.entries()].map(([brand, groups]) => {
    const groupedEntries = [...groups.values()];
    return {
      brand,
      groups: groupedEntries,
      count: groupedEntries.reduce((sum, group) => sum + group.features.length, 0),
    };
  });
}

export function FeatureRemapStudio({ onOpenWorkspace }: FeatureRemapStudioProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedBrand, setSelectedBrand] = useState("All");
  const [selectedFeaturePath, setSelectedFeaturePath] = useState<string | null>(
    FEATURE_REMAPPINGS[0]?.featurePath ?? null,
  );

  const filteredFeatures = useMemo(() => {
    return FEATURE_REMAPPINGS.filter((feature) => {
      const matchesBrand = selectedBrand === "All" || feature.brand === selectedBrand;
      return matchesBrand && featureMatchesQuery(feature, searchQuery);
    });
  }, [searchQuery, selectedBrand]);

  const brandSections = useMemo(() => groupByBrand(filteredFeatures), [filteredFeatures]);

  const selectedFeature = useMemo(() => {
    if (filteredFeatures.length === 0) {
      return null;
    }

    return (
      filteredFeatures.find((feature) => feature.featurePath === selectedFeaturePath) ??
      filteredFeatures[0]
    );
  }, [filteredFeatures, selectedFeaturePath]);

  return (
    <div className="feature-remap-studio">
      <section className="feature-remap-hero glass-card">
        <div className="feature-remap-hero__copy">
          <span className="feature-remap-kicker">Kotlin → Desktop Remapping</span>
          <h2 className="feature-remap-title">Imported Android feature surface is now wired into desktop labs</h2>
          <p className="feature-remap-subtitle">
            The desktop shell now understands every extracted brand, group, and feature entry from the Kotlin inventory and routes it to the most relevant desktop workspace.
          </p>
        </div>

        <div className="feature-remap-stats">
          <div className="feature-remap-stat">
            <span className="feature-remap-stat__value">{FEATURE_SUMMARY.totalFeatures}</span>
            <span className="feature-remap-stat__label">Imported features</span>
          </div>
          <div className="feature-remap-stat">
            <span className="feature-remap-stat__value">{FEATURE_SUMMARY.totalBrands}</span>
            <span className="feature-remap-stat__label">Brand catalogs</span>
          </div>
          <div className="feature-remap-stat">
            <span className="feature-remap-stat__value">{FEATURE_SUMMARY.authRequiredCount}</span>
            <span className="feature-remap-stat__label">Auth-gated ops</span>
          </div>
          <div className="feature-remap-stat">
            <span className="feature-remap-stat__value">{FEATURE_SUMMARY.criticalCount}</span>
            <span className="feature-remap-stat__label">Critical-risk ops</span>
          </div>
        </div>
      </section>

      <section className="feature-remap-toolbar glass-card">
        <label className="feature-remap-search">
          <span>Search imported features</span>
          <input
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search brand, mode, chipset, FRP, IMEI, firmware..."
          />
        </label>

        <div className="feature-remap-brand-filter">
          {BRAND_OPTIONS.map((brand) => (
            <button
              key={brand}
              type="button"
              className={`feature-remap-brand-pill ${selectedBrand === brand ? "active" : ""}`}
              onClick={() => setSelectedBrand(brand)}
            >
              {brand}
            </button>
          ))}
        </div>

        <div className="feature-remap-toolbar__meta">
          <span>{filteredFeatures.length} features visible</span>
          <span>{brandSections.length} brand sections active</span>
        </div>
      </section>

      <div className="feature-remap-layout">
        <div className="feature-remap-catalog">
          {brandSections.length === 0 ? (
            <div className="feature-remap-empty glass-card">
              <h3>No matching features</h3>
              <p>Adjust the search query or brand filter to reveal the imported Kotlin feature mappings.</p>
            </div>
          ) : (
            brandSections.map((section) => (
              <section key={section.brand} className="feature-brand-section glass-card">
                <header className="feature-brand-section__header">
                  <div>
                    <span className="feature-brand-section__eyebrow">Brand workspace</span>
                    <h3>{section.brand}</h3>
                  </div>
                  <span className="feature-brand-section__count">{section.count} mapped features</span>
                </header>

                <div className="feature-group-stack">
                  {section.groups.map((group) => (
                    <div key={group.id} className="feature-group-block">
                      <div className="feature-group-block__header">
                        <h4>{group.title}</h4>
                        <span>{group.features.length} ops</span>
                      </div>

                      <div className="feature-card-grid">
                        {group.features.map((feature) => {
                          const isSelected = selectedFeature?.featurePath === feature.featurePath;
                          return (
                            <article
                              key={feature.featurePath}
                              className={`feature-card feature-card--${riskTone(feature.risk)} ${isSelected ? "feature-card--selected" : ""}`}
                              onClick={() => setSelectedFeaturePath(feature.featurePath)}
                            >
                              <div className="feature-card__header">
                                <div className="feature-card__identity">
                                  <span className="feature-card__icon" aria-hidden="true">
                                    {feature.icon}
                                  </span>
                                  <div>
                                    <h5>{feature.label}</h5>
                                    <p>{feature.description || "Desktop-guided execution path available."}</p>
                                  </div>
                                </div>

                                <div className="feature-card__badges">
                                  <span className={`feature-risk-badge feature-risk-badge--${riskTone(feature.risk)}`}>
                                    {riskLabel(feature.risk)}
                                  </span>
                                  {feature.requiresAuth && <span className="feature-auth-badge">Auth</span>}
                                </div>
                              </div>

                              <div className="feature-chip-row">
                                {feature.modes.map((mode) => (
                                  <span key={`${feature.featurePath}-${mode}`} className="feature-chip feature-chip--mode">
                                    {mode}
                                  </span>
                                ))}
                                {feature.chipsets.map((chipset) => (
                                  <span key={`${feature.featurePath}-${chipset}`} className="feature-chip feature-chip--chipset">
                                    {chipset}
                                  </span>
                                ))}
                              </div>

                              <div className="feature-card__footer">
                                <div>
                                  <span className="feature-card__mapped-label">Mapped desktop lab</span>
                                  <strong>{feature.workspaceLabel}</strong>
                                </div>

                                <button
                                  type="button"
                                  className="feature-card__action"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    onOpenWorkspace(feature.workspaceId, feature);
                                  }}
                                >
                                  Open
                                </button>
                              </div>
                            </article>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            ))
          )}
        </div>

        <aside className="feature-remap-inspector glass-card">
          {selectedFeature ? (
            <>
              <div className="feature-remap-inspector__header">
                <span className="feature-remap-inspector__eyebrow">Selected mapping</span>
                <h3>{selectedFeature.label}</h3>
                <p>{selectedFeature.brand} · {selectedFeature.groupTitle}</p>
              </div>

              <div className="feature-remap-inspector__section">
                <span className="feature-remap-inspector__label">Execution guidance</span>
                <p>{selectedFeature.executionHint}</p>
              </div>

              <div className="feature-remap-inspector__section">
                <span className="feature-remap-inspector__label">Primary desktop lab</span>
                <button
                  type="button"
                  className="feature-remap-inspector__primary"
                  onClick={() => onOpenWorkspace(selectedFeature.workspaceId, selectedFeature)}
                >
                  {WORKSPACE_META[selectedFeature.workspaceId].icon} {selectedFeature.workspaceLabel}
                </button>
              </div>

              <div className="feature-remap-inspector__section">
                <span className="feature-remap-inspector__label">Alternate compatible labs</span>
                <div className="feature-remap-inspector__candidates">
                  {selectedFeature.workspaceCandidates.map((candidate) => (
                    <button
                      key={`${selectedFeature.featurePath}-${candidate}`}
                      type="button"
                      className="feature-remap-inspector__candidate"
                      onClick={() => onOpenWorkspace(candidate, selectedFeature)}
                    >
                      {WORKSPACE_META[candidate].icon} {WORKSPACE_META[candidate].label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="feature-remap-inspector__section">
                <span className="feature-remap-inspector__label">Automation bridge</span>
                <div className="feature-remap-inspector__terminal">
                  {selectedFeature.commandHint ?? "Guided routing only — use the mapped workspace UI."}
                </div>
                <span className="feature-remap-inspector__meta">{selectedFeature.automation.toUpperCase()} DESKTOP FLOW</span>
              </div>

              {selectedFeature.warningMsg && (
                <div className="feature-remap-warning">
                  <span>Warning</span>
                  <p>{selectedFeature.warningMsg}</p>
                </div>
              )}

              {selectedFeature.successLog && (
                <div className="feature-remap-success">
                  <span>Success target</span>
                  <p>{selectedFeature.successLog}</p>
                </div>
              )}
            </>
          ) : (
            <div className="feature-remap-empty">
              <h3>No feature selected</h3>
              <p>Select any mapped feature card to inspect its desktop execution path.</p>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
