pipeline {
    agent {
        docker { 
            image 'empear/codescene-ci-cd:latest' 
            args '--entrypoint='
        }
    }
    environment {
        CODESCENE_USER = credentials('CODESCENE_USER')
        CODESCENE_PASSWORD = credentials('CODESCENE_PASSWORD')
        GITHUB_TOKEN = credentials('GITHUB_TOKEN')
    }
    stages {
        stage('Run Delta Analysis') {
            steps {
                sh '''
                  CODESCENE_DELTA_ANALYSIS_URL=http://host.docker.internal:3003/projects/20/delta-analysis
                  if [[ -z "$CODESCENE_DELTA_ANALYSIS_URL" ]] ; then
                    echo "No value specified for CODESCENE_DELTA_ANALYSIS_URL!"
                    exit 1
                  fi
                  if [[ -z "$CODESCENE_USER" ]] ; then
                    echo "No value specified for CODESCENE_USER!"
                    exit 1
                  fi
                  if [[ -z "$CODESCENE_PASSWORD" ]] ; then
                    echo "No value specified for CODESCENE_PASSWORD!"
                    exit 1
                  fi
                  if [[ -z "$CHANGE_ID" ]] ; then
                    echo "No value specified for CHANGE_ID!"
                    exit 1
                  fi
		  ORIGIN_URL=$(git config --get remote.origin.url)
		  REPO_NAME=$(basename ${ORIGIN_URL} .git)
		  OWNER=$(echo ${ORIGIN_URL} | sed -E -e 's#/[^/]*$##' -e 's#.*/##')
                  codescene-ci-cd.sh \
                    --codescene-delta-analysis-url ${CODESCENE_DELTA_ANALYSIS_URL} \
                    --codescene-user ${CODESCENE_USER} \
                    --codescene-password ${CODESCENE_PASSWORD} \
                    --codescene-repository ${REPO_NAME} \
                    --fail-on-failed-goal \
                    --fail-on-declining-code-health \
                    --analyze-branch-diff \
                    --current-commit ${GIT_COMMIT} \
                    --base-revision origin/${CHANGE_TARGET} \
		    --create-github-comment \
		    --github-api-url "https://api.github.com" \
		    --github-api-token ${GITHUB_TOKEN} \
		    --github-owner empear-analytics \
		    --github-repo ${REPO_NAME} \
		    --github-pull-request-id ${CHANGE_ID} \
                    --log-result
                    '''
            }
        }
    }
}
