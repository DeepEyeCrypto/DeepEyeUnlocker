/**
 * PageSkeleton - Loading placeholder for lazy-loaded pages
 * Mimics the page layout structure while content loads
 */
export function PageSkeleton() {
  return (
    <div className="page" style={{ opacity: 0.8, minHeight: '100vh', padding: '1.5rem' }}>
      {/* Loading indicator */}
      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        marginBottom: '2rem',
        color: '#60a5fa',
        fontSize: '0.9rem',
        fontFamily: 'monospace'
      }}>
        <span style={{ 
          display: 'inline-block',
          animation: 'spin 1s linear infinite',
          marginRight: '0.5rem'
        }}>⚙️</span>
        Loading module...
      </div>
      
      {/* Title skeleton */}
      <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 16 }}>
        <div
          style={{
            height: 28,
            width: "40%",
            background: "var(--color-bg-elevated)",
            borderRadius: "var(--radius-md)",
            animation: "pulse 1.5s ease-in-out infinite",
          }}
        />
        <div
          style={{
            height: 14,
            width: "60%",
            background: "var(--color-bg-elevated)",
            borderRadius: "var(--radius-sm)",
            animation: "pulse 1.5s ease-in-out infinite",
          }}
        />
      </div>

      {/* Card skeletons */}
      <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="card"
            style={{
              padding: 16,
              animation: "pulse 1.5s ease-in-out infinite",
              animationDelay: `${i * 0.15}s`,
            }}
          >
            <div
              style={{
                height: 16,
                width: "30%",
                background: "var(--color-bg-elevated)",
                borderRadius: "var(--radius-sm)",
                marginBottom: 12,
              }}
            />
            <div
              style={{
                height: 12,
                width: "80%",
                background: "var(--color-bg-elevated)",
                borderRadius: "var(--radius-sm)",
              }}
            />
          </div>
        ))}
      </div>
    </div>
  );
}
