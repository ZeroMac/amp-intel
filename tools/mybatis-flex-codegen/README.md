# MyBatis-Flex Codegen

Development-time code generator for service entities and mappers.

## Database environment

Set the database connection in the local shell. Do not commit credentials.

```powershell
$env:CODEGEN_DB_URL="jdbc:postgresql://localhost:5432/hldb01"
$env:CODEGEN_DB_USERNAME="hl_user"
$env:CODEGEN_DB_PASSWORD="hlpassword"
```

## Run

From the repository root:

```powershell
.\tools\codegen.ps1 system
```

If the profile argument is omitted, `system` is used.

Profiles are stored in `src/main/resources/profiles`. A profile defines the target module, base package, schema, table prefix, and generated tables.

The generator currently creates Entity and Mapper source files only. MyBatis-Flex APT remains responsible for generated TableDef sources during normal service compilation.
