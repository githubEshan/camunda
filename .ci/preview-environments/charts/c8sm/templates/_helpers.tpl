{{ define "commonLabels" -}}
{{- toYaml .Values.global.labels -}}
{{ end }}

{{ define "commonAnnotations" -}}
camunda.cloud/created-by: "{{ .Values.git.repoUrl }}/blob/{{ .Values.git.branch }}/.ci/{{ .Template.Name }}"
{{ end }}
