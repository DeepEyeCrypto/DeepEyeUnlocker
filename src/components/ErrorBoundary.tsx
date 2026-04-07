import { Component, type ErrorInfo, type ReactNode } from "react";

type ErrorBoundaryProps = {
  children: ReactNode;
};

type ErrorBoundaryState = {
  error: Error | null;
  componentStack: string;
};

export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  public constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      error: null,
      componentStack: "",
    };
  }

  public static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return {
      error,
      componentStack: "",
    };
  }

  public componentDidCatch(_error: Error, info: ErrorInfo): void {
    this.setState({ componentStack: info.componentStack ?? "" });
  }

  public render() {
    const { error, componentStack } = this.state;

    if (!error) {
      return this.props.children;
    }

    const stackTrace = [error.stack ?? "", componentStack]
      .filter((entry) => entry.length > 0)
      .join("\n\n");

    return (
      <div className="error-boundary-screen">
        <div className="error-boundary-card">
          <p className="error-boundary-badge">Application crash intercepted</p>
          <h1 className="error-boundary-title">DeepEyeUnlocker recovered from a UI fault</h1>
          <p className="error-boundary-message">{error.message}</p>

          <details className="error-boundary-details">
            <summary>Stack trace</summary>
            <pre className="error-boundary-stack">{stackTrace}</pre>
          </details>

          <button
            type="button"
            className="btn btn-primary btn-md"
            onClick={() => window.location.reload()}
          >
            Reload App
          </button>
        </div>
      </div>
    );
  }
}
