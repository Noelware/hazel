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

window.$log.info('Determing CDN version...');

(async() => {
  const bits = new URL(window.location.href);
  const data = await fetch(`${bits.protocol}://${bits.hostname}${bits.port}/info`, {
    headers: {
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());

  if (!data.success) {
    const error = new Error(`Unable to request to ${bits.protocol}://${bits.hostname}${bits.port}/info`);
    error.data = data.errors.map(error => `   - ${error.code}: ${error.message}`);

    window.$log.error(`Unable to request to ${bits.protocol}://${bits.hostname}${bits.port}/info:`, error);
    throw error;
  }

  window.$log.info(JSON.stringify(data));
})();
