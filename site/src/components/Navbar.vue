<!--
~ 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
~ Copyright 2022 Noel <cutie@floofy.dev>
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~     http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" BASIS,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
-->

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { MenuIcon } from '@heroicons/vue/solid';

const navMenuRef = ref<HTMLDivElement>();
const buttonRef = ref<HTMLElement>();

onMounted(() => {
  const $ref = navMenuRef.value!;
  const btnRef = buttonRef.value!;

  btnRef.addEventListener('click', () => {
    $ref.classList.toggle('hidden');
  });
});
</script>

<template>
  <header>
    <nav
      class="dark:bg-[#232323] bg-zinc-100 flex flex-wrap items-center justify-between w-full py-4 md:h-16 md:py-0 px-4 text-lg shadow-md"
    >
      <div class="flex flex-row">
        <img src="/favicon.ico" alt="hazel feather" draggable="false" />
        <span class="font-cantarell text-xl font-semibold ml-3 mt-[1.5px]">hazel | ヘイゼル</span>
      </div>

      <MenuIcon
        ref="buttonRef"
        class="h-6 w-6 cursor-pointer md:hidden block focus:ring-pink-400 focus:ring-2 focus:rounded-md"
      />

      <div ref="navMenuRef" class="w-full hidden lg:flex lg:items-center lg:w-auto">
        <div class="text-base lg:flex-grow">
          <a
            href="/"
            class="block mt-4 lg:inline-block lg:mt-0 mr-4 p-2 focus:ring-pink-400 focus:ring-2 focus:rounded-md"
          >
            Home
          </a>
        </div>

        <div class="text-base lg:flex-grow">
          <a
            href="/docs"
            class="block mt-4 lg:inline-block lg:mt-0 mr-4 p-2 focus:ring-pink-400 focus:ring-2 focus:rounded-md"
          >
            Documentation
          </a>
        </div>

        <div class="text-base lg:flex-grow">
          <a
            href="https://github.com/auguwu/hazel"
            class="block mt-4 lg:inline-block lg:mt-0 mr-4 p-2 focus:ring-pink-400 focus:ring-2 focus:rounded-md"
            target="_blank"
          >
            GitHub
          </a>
        </div>
      </div>
    </nav>
  </header>
</template>

<!--
  <div class="w-full block flex-grow lg:flex lg:items-center lg:w-auto">
    <div class="text-sm lg:flex-grow">
      <a href="#responsive-header" class="block mt-4 lg:inline-block lg:mt-0 text-teal-200 hover:text-white mr-4">
        Docs
      </a>
      <a href="#responsive-header" class="block mt-4 lg:inline-block lg:mt-0 text-teal-200 hover:text-white mr-4">
        Examples
      </a>
      <a href="#responsive-header" class="block mt-4 lg:inline-block lg:mt-0 text-teal-200 hover:text-white">
        Blog
      </a>
    </div>
    <div>
      <a href="#" class="inline-block text-sm px-4 py-2 leading-none border rounded text-white border-white hover:border-transparent hover:text-teal-500 hover:bg-white mt-4 lg:mt-0">Download</a>
    </div>
  </div>
-->
