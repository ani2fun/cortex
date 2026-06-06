// Re-exports the static portfolio JSON files so the Scala.js client can
// import them through a single ESM module. Vite inlines JSON imports as
// JavaScript objects at bundle time, so the data is shipped with the SPA
// — no runtime fetch needed for the home page.
import projects from "./projectsData.json";
import experience from "./experienceData.json";
import certifications from "./certificationsData.json";

export { projects, experience, certifications };
