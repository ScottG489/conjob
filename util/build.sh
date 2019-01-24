#!/bin/bash
set -e

chmod 400 /root/.ssh/id_rsa

git clone git@github.com:ScottG489/docker-ci-prototype.git
cd docker-ci-prototype
./gradlew build fatCapsule

docker build -t scottg489/docker-ci-prototype:latest .
docker push scottg489/docker-ci-prototype:latest

# Things below we may only want to run once and not for every deployment
APP_NAME=docker-ci-prototype
aws s3 mb s3://$APP_NAME || true
aws s3 cp util/docker_app.aws.json s3://$APP_NAME/docker_app.aws.json

# Creates application as well with given file
aws elasticbeanstalk create-application-version --application-name $APP_NAME --version-label version_lbl_1 --auto-create-application --process --source-bundle S3Bucket=$APP_NAME,S3Key=docker_app.aws.json || true
# create template to refer to when creating environment
aws elasticbeanstalk create-configuration-template --application-name $APP_NAME --template-name template_name_1 --solution-stack-name "64bit Amazon Linux 2018.03 v2.12.6 running Docker 18.06.1-ce" || true
# dns name needs to be globally unique
# can use aws elasticbeanstalk check-dns-availability --cname-prefix my-cname to check if it's available
aws elasticbeanstalk create-environment --cname-prefix $APP_NAME --application-name $APP_NAME --template-name template_name_1 --version-label version_lbl_1 --environment-name $APP_NAME-env || true
