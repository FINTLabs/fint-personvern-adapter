#!/usr/bin/env python3
"""
Generates kustomize/overlays/<environment>/ from the org lists below.

One deployment per environment serves every org as a tenant. Each org gets a FintAdapter credential
(the operator stores it in a secret named after the resource) that is wired into the Application as
FINT_ADAPTER_TENANTS_<n>_* environment variables. Run after changing ENVIRONMENTS, then commit the output.
"""
from pathlib import Path

ENVIRONMENTS = {
    "alpha": {
        "base_url": "https://alpha.felleskomponent.no",
        "orgs": ["fintlabs.no"],
    },
    "beta": {
        "base_url": "https://beta.felleskomponent.no",
        "orgs": ["agderfk.no", "rogfk.no", "vestfoldfylke.no", "trondelagfylke.no", "fintlabs.no"],
    },
    "api": {
        "base_url": "https://api.felleskomponent.no",
        "orgs": ["agderfk.no", "rogfk.no", "vestfoldfylke.no", "trondelagfylke.no", "fintlabs.no"],
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


def fint_adapters(orgs: list[str]) -> str:
    docs = []
    for org in orgs:
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


def env_patch(base_url: str, orgs: list[str]) -> str:
    env = [
        ("JAVA_TOOL_OPTIONS", "'-XX:+ExitOnOutOfMemoryError -XX:MaxRAMPercentage=75'"),
        ("fint.adapter.base-url", f"'{base_url}'"),
    ]
    lines = ["- op: replace", "  path: /spec/env", "  value:"]
    for name, value in env:
        lines += [f"    - name: {name}", f"      value: {value}"]
    for index, org in enumerate(orgs):
        lines += [
            f"    - name: FINT_ADAPTER_TENANTS_{index}_ORGID",
            f"      value: '{org}'",
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
        (directory / "fint-adapters.yaml").write_text(fint_adapters(spec["orgs"]))
        (directory / "application-env.yaml").write_text(env_patch(spec["base_url"], spec["orgs"]))
        print(f"{environment}: {len(spec['orgs'])} tenant(s)")


if __name__ == "__main__":
    main()
