This directory contains build scripts used to promote changes to production:

1. Project-CB-Build: The continuous build, triggered on each push. Quick feedback loop.
2. Project-CI-Build: The comprehensive build, deriving shippable release versions. Core content is also inspecting source code (with SonarQube) and Binaries (with Twistlock), and distributing artifacts to component repository.
3. Project-RC-Build: Cherry-picking defined versions to be release candidates. 
4. Project-GA-Build: Cherry-picking release candidates to be general availability versions.
5. oracle-cloud: Automatically deploys the business application in the Oracle Cloud, based on Docker.
