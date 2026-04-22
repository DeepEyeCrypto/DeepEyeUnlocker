import { Component, ReactNode } from "react"

interface Props { children: ReactNode }
interface State { error: Error | null }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  render() {
    if (this.state.error) {
      return (
        <div style={{
          padding: 24, color: "#ff6b6b",
          fontFamily: "monospace",
          background: "#1a1a1a"
        }}>
          <h3>❌ Component Error</h3>
          <pre style={{fontSize: 12}}>
            {this.state.error.message}
          </pre>
          <button onClick={() => this.setState({ error: null })}
            style={{
              marginTop: '1rem',
              padding: '0.5rem 1rem',
              backgroundColor: '#2b2b2b',
              color: '#fff',
              border: '1px solid #444',
              borderRadius: '4px',
              cursor: 'pointer'
            }}>
            🔄 Retry
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
