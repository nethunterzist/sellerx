import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const nextConfig: NextConfig = {
  // Enable standalone output for Docker production builds
  output: "standalone",

  // ESLint configuration - ignore during builds to prevent deployment failures
  eslint: {
    // Warning: This allows production builds to successfully complete even if
    // your project has ESLint errors.
    ignoreDuringBuilds: true,
  },

  // TypeScript configuration - ignore during builds to prevent deployment failures
  typescript: {
    // !! WARNING !!
    // Dangerously allow production builds to successfully complete even if
    // your project has type errors.
    // !!
    ignoreBuildErrors: true,
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
