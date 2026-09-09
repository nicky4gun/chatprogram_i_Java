I README skal I beskrive 3-5 væsentlige situationer, hvor AI har haft betydning for jeres arbejde: 

 

Opgave 

AI-værktøj 

AI’s forslag 

Jeres vurdering og ændringer 

Kontrol og test 

 

 

 

 

 

 

 

 

 

 

 
Test 

Følgende scenarier skal som minimum afprøves: 

Scenarie 

Forventet resultat 

Tre klienter forbindes samtidig 

Alle klienter kan sende og modtage beskeder 

To brugere vælger samme brugernavn 

Den anden bruger afvises 

En bruger sender en besked i et rum 

Kun brugere i det pågældende rum modtager beskeden 

En bruger sender en privat besked 

Kun den valgte modtager modtager beskeden 

En klient sender en fejlformateret besked 

Serveren sender en fejl og fortsætter med at køre 

En klient lukker uventet 

Brugeren fjernes fra serverens samlinger 

Den valgte udvidelse anvendes 

Udvidelsen fungerer som beskrevet 

MessageParser og centrale dele af beskedhåndteringen skal testes automatisk med JUnit. Testen med flere samtidige klienter må gerne gennemføres og dokumenteres som en manuel integrationstest.

README skal indeholde en vejledning til at starte server og klient, en beskrivelse af protokollen, et klassediagram, en forklaring af trådmodellen og de delte ressourcer, testresultater, AI-dokumentation og en beskrivelse af den valgte udvidelse.


Diagrammer i README 

Diagrammerne i opgaven viser en mulig startstruktur og et eksempel på kommunikationen. Inden aflevering skal gruppen: 

opdatere klassediagrammet, så det passer til den færdige kode og  

udarbejde ét sekvensdiagram, som viser enten den valgte udvidelse eller et fejlsætningsforløb  

 

Et fejlsætningsforløb kan eksempelvis vise, hvad der sker, når et brugernavn er optaget, en privat modtager ikke findes, eller en klient mister forbindelsen. 

Diagrammerne må gerne laves med Mermaid/PlantUML og indsættes direkte i README. 
