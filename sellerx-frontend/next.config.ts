import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

// Backend URL for API and WebSocket proxy
const BACKEND_URL = process.env.API_BASE_URL || "http://localhost:8080";

const nextConfig: NextConfig = {
  // Enable standalone output for Docker production builds
  output: "standalone",

  // Generate unique build ID to invalidate old chunks
  generateBuildId: async () => {
    return `build-${Date.now()}`;
  },

  // ESLint configuration
  eslint: {
    ignoreDuringBuilds: false,
  },

  // TypeScript configuration
  typescript: {
    ignoreBuildErrors: false,
  },

  // API and WebSocket proxy rewrites
  async rewrites() {
    return [
      // WebSocket proxy for real-time communications
      {
        source: "/api/ws",
        destination: `${BACKEND_URL}/ws`,
      },
      {
        source: "/api/ws/:path*",
        destination: `${BACKEND_URL}/ws/:path*`,
      },
    ];
  },

  // Security and cache headers
  async headers() {
    const securityHeaders = [
      { key: "X-Frame-Options", value: "DENY" },
      { key: "X-Content-Type-Options", value: "nosniff" },
      { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
      {
        key: "Permissions-Policy",
        value: "camera=(), microphone=(), geolocation=()",
      },
      {
        key: "Content-Security-Policy",
        value:
          "default-src 'self'; " +
          "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
          "style-src 'self' 'unsafe-inline'; " +
          "img-src 'self' data: https: blob:; " +
          "font-src 'self' data:; " +
          "connect-src 'self' https://api.trendyol.com https://*.sslip.io ws: wss:; " +
          "frame-ancestors 'none';",
      },
      {
        key: "Strict-Transport-Security",
        value: "max-age=31536000; includeSubDomains; preload",
      },
    ];

    return [
      // Static assets - uzun cache (chunk hash'leri unique)
      {
        source: "/_next/static/:path*",
        headers: [
          { key: "Cache-Control", value: "public, max-age=31536000, immutable" },
        ],
      },
      // HTML sayfaları - her zaman fresh manifest al (chunk hatasını önler)
      {
        source: "/:path((?!_next/static|_next/image|favicon.ico).*)",
        headers: [
          { key: "Cache-Control", value: "no-store, must-revalidate" },
          ...securityHeaders,
        ],
      },
      // Fallback - security headers
      {
        source: "/(.*)",
        headers: securityHeaders,
      },
    ];
  },

  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "lh3.googleusercontent.com",
        port: "",
        pathname: "/**",
      },
      {
        protocol: "https",
        hostname: "cdn.dsmcdn.com",
        port: "",
        pathname: "/**",
      },
      {
        protocol: "https",
        hostname:
          "marketplace-single-product-images.oss-eu-central-1.aliyuncs.com",
        port: "",
        pathname: "/**",
      },
    ],
  },

  // Development optimizations
  experimental: {
    // Enable hot reloading in Docker
    turbo: {
      resolveAlias: {
        underscore: "lodash",
      },
      resolveExtensions: [".mdx", ".tsx", ".ts", ".jsx", ".js", ".json"],
    },
  },

  // Docker development settings
  ...(process.env.NODE_ENV === "development" && {
    // Enable file watching in Docker
    webpack: (config, { dev }) => {
      if (dev) {
        config.watchOptions = {
          poll: 1000,
          aggregateTimeout: 300,
        };
      }
      return config;
    },
  }),
};

const withNextIntl = createNextIntlPlugin();
export default withNextIntl(nextConfig);
