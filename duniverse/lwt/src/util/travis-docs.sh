set -x

date

BUILD_DOCS=yes
[ "$DOCS" == yes ] || BUILD_DOCS=no
[ "$TRAVIS_BRANCH" == master ] || BUILD_DOCS=no
[ "$TRAVIS_PULL_REQUEST" == false ] || BUILD_DOCS=no
[ "$TRAVIS_EVENT_TYPE" != cron ] || BUILD_DOCS=no
[ "$TRAVIS_REPO_SLUG" == ocsigen/lwt ] || BUILD_DOCS=no
[ "$TRAVIS_TAG" == "" ] || BUILD_DOCS=no

if [ "$BUILD_DOCS" == yes ]
then
    opam pin add -y wikidoc git+https://github.com/ocsigen/wikidoc.git
    opam install --unset-root -y uchar
    make doc-api-wiki

    set +x
    echo $DOCS_DEPLOY_KEY | base64 --decode > ~/.ssh/docs
    chmod 400 ~/.ssh/docs
    echo >> ~/.ssh/config
    echo "Host github.com" >> ~/.ssh/config
    echo "  IdentityFile ~/.ssh/docs" >> ~/.ssh/config
    echo "  StrictHostKeyChecking no" >> ~/.ssh/config
    set -x

    date

    git clone git@github.com:ocsigen/lwt.git lwt-docs
    cd lwt-docs
    git config user.name "Anton Bachin"
    git config user.email "antonbachin@yahoo.com"
    git checkout wikidoc
    rm -rf docs/dev
    mkdir -p docs/dev/api
    mkdir -p docs/dev/manual
    cp -r ../docs/api/wiki/* docs/dev/api
    cp ../docs/*.wiki docs/dev/manual

    date

    git add -A
    if ! git diff-index --quiet --exit-code HEAD
    then
        MESSAGE="Development docs"
        LAST=`git log -1 --pretty=%B | head -n 1`
        if [ "$LAST" == "$MESSAGE" ]
        then
            AMEND=--amend
        else
            AMEND=
        fi
        git commit $AMEND -m "$MESSAGE"
        git push --force-with-lease
    fi

    cd ..

    date

    opam pin remove --no-action wikidoc
    opam remove -y lwt
fi

date
