/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                deeper: "#0b0f19",
                deepm: "#151b2b",
                deephigh: "#20283c",
                deepaccent: "#3b82f6", // Blue primary for generic, could be adaptable
            },
        },
    },
    plugins: [],
}
