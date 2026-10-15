#!/usr/bin/env python3
"""
Generates kustomize/overlays/<environment>/ from the org lists below.

One deployment per environment serves every org as a tenant. Each credential gets a FintAdapter resource
(the operator stores it in a secret named after the resource) that is wired into the Application as
FINT_ADAPTER_TENANTS_<n>_* environment variables. A credential serves its main org and any sub-orgs listed
with it. Run after changing ENVIRONMENTS, then commit the output.
"""
from pathlib import Path

# "credentials": one entry per adapter credential: the main org (the FintAdapter's orgId) and the orgs it
# serves, which must all be that org or sub-orgs of it.
ENVIRONMENTS = {
    "alpha": {
        "base_url": "https://alpha.felleskomponent.no",
        "credentials": [("fintlabs.no", ["fintlabs.no"])],
    },
    "beta": {
        "base_url": "https://beta.felleskomponent.no",
        "credentials": [(org, [org]) for org in ["agderfk.no", "rogfk.no", "vestfoldfylke.no", "trondelagfylke.no", "fintlabs.no"]],
    },
    "api": {
        "base_url": "https://api.felleskomponent.no",
        "credentials": [(org, [org]) for org in ["agderfk.no", "rogfk.no", "vestfoldfylke.no", "trondelagfylke.no", "fintlabs.no"]],
    },
}

NAMESPACE = "fintlabs-no"
COMPONENTS = ["personvern_kodeverk", "personvern_samtykke"]
SECRET_KEYS = {
    "USERNAME": "fint.core.oauth2.username",
    "PASSWORD": "fint.core.oauth2.password",
    "CLIENTID": "fint.core.oauth2.client-id",
    "CLIENTSECRET": "fint.core.oauth2.client-secret",
}


def dashed(org: str) -> str:
    return org.replace(".", "-")


def credential_name(org: str) -> str:
    return f"personvern-adapter-{dashed(org)}"


def fint_adapters(credentials: list[tuple[str, list[str]]]) -> str:
    docs = []
    for org, _ in credentials:
        docs.append(f"""apiVersion: fintlabs.no/v1alpha1
kind: FintAdapter
metadata:
  name: {credential_name(org)}
  namespace: {NAMESPACE}
  labels:
    app.kubernetes.io/name: fint-personvern-adapter
    app.kubernetes.io/instance: fint-personvern-adapter_fintlabs_no
    app.kubernetes.io/version: latest
    app.kubernetes.io/component: adapter
    app.kubernetes.io/part-of: fint-core
    fintlabs.no/team: core
    fintlabs.no/org-id: {org}
spec:
  orgId: {org}
  note: Personvern adapter credential for {org} (tenant of fint-personvern-adapter)
  components:
""" + "".join(f"    - {c}\n" for c in COMPONENTS))
    return "---\n".join(docs)


def env_patch(base_url: str, credentials: list[tuple[str, list[str]]]) -> str:
    env = [
        ("JAVA_TOOL_OPTIONS", "'-XX:+ExitOnOutOfMemoryError -XX:MaxRAMPercentage=75'"),
        ("fint.adapter.base-url", f"'{base_url}'"),
    ]
    lines = ["- op: replace", "  path: /spec/env", "  value:"]
    for name, value in env:
        lines += [f"    - name: {name}", f"      value: {value}"]
    for index, (org, orgs) in enumerate(credentials):
        lines += [
            f"    - name: FINT_ADAPTER_TENANTS_{index}_ORGS",
            f"      value: '{','.join(orgs)}'",
            f"    - name: FINT_ADAPTER_TENANTS_{index}_ID",
            f"      value: 'https://vigoiks.no/{dashed(org)}/personvern'",
        ]
        for suffix, key in SECRET_KEYS.items():
            lines += [
                f"    - name: FINT_ADAPTER_TENANTS_{index}_{suffix}",
                "      valueFrom:",
                "        secretKeyRef:",
                f"          name: {credential_name(org)}",
                f"          key: {key}",
            ]
    return "\n".join(lines) + "\n"


def kustomization() -> str:
    return f"""apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: {NAMESPACE}
resources:
  - ../../base
  - fint-adapters.yaml
patches:
  - path: application-env.yaml
    target:
      kind: Application
      name: fint-personvern-adapter
"""


def main() -> None:
    root = Path(__file__).parent / "overlays"
    for environment, spec in ENVIRONMENTS.items():
        directory = root / environment
        directory.mkdir(parents=True, exist_ok=True)
        (directory / "kustomization.yaml").write_text(kustomization())
        (directory / "fint-adapters.yaml").write_text(fint_adapters(spec["credentials"]))
        (directory / "application-env.yaml").write_text(env_patch(spec["base_url"], spec["credentials"]))
        orgs = sum(len(orgs) for _, orgs in spec["credentials"])
        print(f"{environment}: {len(spec['credentials'])} credential(s), {orgs} org(s)")


if __name__ == "__main__":
    main()
