using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace DeepEyeUnlocker.Core.HIL
{
    public class HilHtmlReporter
    {
        public static string GenerateReport(ValidationResult result, string deviceName, string protocol)
        {
            var sb = new StringBuilder();
            sb.Append("<!DOCTYPE html><html><head>");
            sb.Append("<style>body{font-family:sans-serif;background:#1a1a1a;color:#eee;} .card{background:#2a2a2a;padding:20px;margin:10px;border-radius:8px;} .pass{color:#4caf50;} .fail{color:#f44336;}</style>");
            sb.Append("</head><body>");
            sb.Append($"<h1>HIL Validation Report: {deviceName}</h1>");
            sb.Append($"<div class='card'><h2>Protocol: {protocol}</h2>");
            sb.Append($"<p>Status: <span class='{(result.IsMatch ? "pass" : "fail")}'>{(result.IsMatch ? "PASSED" : "FAILED")}</span></p>");
            sb.Append($"<p>Similarity: {result.SimilarityScore:P2}</p>");
            sb.Append($"<p>Recommendation: {result.Recommendation}</p></div>");

            if (result.Differences.Count > 0)
            {
                sb.Append("<div class='card'><h2>Mismatches</h2><table>");
                sb.Append("<tr><th>Step</th><th>Type</th><th>Label</th><th>Details</th></tr>");
                foreach (var diff in result.Differences)
                {
                    sb.Append($"<tr><td>{diff.StepIndex}</td><td>{diff.DifferenceType}</td><td>{diff.Label}</td>");
                    sb.Append($"<td>Exp: {diff.ExpectedHex} <br/> Act: {diff.ActualHex}</td></tr>");
                }
                sb.Append("</table></div>");
            }

            sb.Append("</body></html>");
            return sb.ToString();
        }
    }
}
