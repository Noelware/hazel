/*
 * 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
 * Copyright 2022 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// @ts-check

/** @type {import('tailwindcss/tailwind-config').TailwindConfig} */
const defaultConfig = require('tailwindcss/defaultConfig');

/**
 * Tailwind configuration for Pak.
 * @type {import('tailwindcss/tailwind-config').TailwindConfig}
 */
const tailwindConfig = {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        // @ts-ignore
        sans: ['Inter', ...defaultConfig.theme.fontFamily.sans],

        // @ts-ignore
        mono: ['"JetBrains Mono"', ...defaultConfig.theme.fontFamily.mono],

        // @ts-ignore
        cantarell: ['Cantarell', ...defaultConfig.theme.fontFamily.sans]
      }
    }
  },
  darkMode: 'media',
  variants: {},
  plugins: [require('@tailwindcss/typography')]
};

module.exports = tailwindConfig;
