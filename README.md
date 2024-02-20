### GenRand

Web app to conveniently find and access pseudorandom permutation functions for any use case. You can see it yourself at [genrand.liamw.net](https://genrand.liamw.net)

GenRand is written in two halves, a Blazor/C# frontend, attached to a Java/Spring Boot backend. SQLite is used for storage of the functions. The front end manages the display of the mixing functions and lists, while the backend handles serving functions from the SQLite database, as well as generating new mix functions (eventually, attempting to cover the entire space of possibilities)

If you want to run it yourself, you'll need Docker. The backend binds to port 65480 and the frontend to port 65380. You'll need to mount a shared directory at `/runtime/images` in the Java container and at `/app/wwwroot/images` in the C# container, else you will not see any visual graphs of the avalanche functions. For Java, you'll also need to mount `./genrand.db` if you want a persistent database.